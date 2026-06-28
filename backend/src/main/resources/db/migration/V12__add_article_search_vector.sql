-- 1. Add a new column to the articles table to store the pre-processed search data.
--    tsvector is a special PostgreSQL type — a sorted list of lexemes (processed words).
--    We are NOT computing this on every search query; we store it once and update it via a trigger.

ALTER TABLE articles ADD COLUMN search_vector tsvector;

-- 2. Create a PostgreSQL function (written in PL/pgSQL) that builds the tsvector.
--    This function will be called automatically by the trigger below on every INSERT and UPDATE.
--    NEW refers to the row being inserted or updated.
--
--    setweight assigns relevance priority to each field:
--      'A' = heading  (highest weight — a match here means it's a very relevant article)
--      'B' = summary  (medium weight)
--      'C' = content  (lowest weight — a match deep in the body is less significant)
--
--    to_tsvector('english', ...) processes the text:
--      - strips stop words (the, a, is, are...)
--      - stems words (running → run, articles → articl)
--      - lowercases everything
--
--    COALESCE(NEW.summary, '') handles the fact that summary is nullable.
--    If summary is NULL, we use empty string so the whole expression doesn't become NULL.
--
--    || is the tsvector concatenation operator — combines all three weighted vectors into one.
--
--    NEW.search_vector := ... assigns the result back to the row before it's saved.
--    RETURN NEW; tells PostgreSQL to proceed with saving the row.

CREATE OR REPLACE FUNCTION update_article_search_vector()
RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english',COALESCE(NEW.heading,'')),'A') ||
        setweight(to_tsvector('english',COALESCE(NEW.summary,'')),'B') ||
        setweight(to_tsvector('english',COALESCE(NEW.content,'')),'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 3. Create the trigger that calls the function above.
--    BEFORE INSERT OR UPDATE means: run this before every insert and every update on articles.
--    FOR EACH ROW means: run it once per row (not once per statement).
--    So whenever someone creates or edits an article, search_vector is automatically rebuilt.
CREATE TRIGGER articles_search_vector_trigger
    BEFORE INSERT OR UPDATE ON articles
    FOR EACH ROW EXECUTE FUNCTION update_article_search_vector();

-- 4. Backfill existing articles.
--    The trigger only fires on future inserts/updates. Rows that already exist in the database
--    still have search_vector = NULL. This UPDATE populates them all right now.
UPDATE articles
SET search_vector = 
    setweight(to_tsvector('english',COALESCE(heading,'')),'A') ||
    setweight(to_tsvector('english',COALESCE(summary,'')),'B') ||
    setweight(to_tsvector('english',COALESCE(content,'')),'C');

-- 5. Create the GIN index on the search_vector column.
--    Without this, every search would scan every row (full table scan = slow).
--    GIN (Generalized Inverted Index) maps each lexeme to the rows that contain it.
--    Searching becomes a fast set lookup instead of a scan.
--    This index is built AFTER the backfill so it doesn't have to be updated row-by-row.
CREATE INDEX idx_articles_search_vector ON articles USING GIN(search_vector);


