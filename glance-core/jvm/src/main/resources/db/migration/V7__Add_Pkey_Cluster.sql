ALTER TABLE cluster_session
  ADD COLUMN id UUID,
  DROP COLUMN cluster_id;

ALTER TABLE cluster_session ADD PRIMARY KEY (id);

ALTER TABLE recap_session
  DROP COLUMN recap_item;