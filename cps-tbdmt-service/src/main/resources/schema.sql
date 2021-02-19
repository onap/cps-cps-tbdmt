CREATE TABLE Template(
	templateId TEXT NOT NULL,
	model TEXT NOT NULL,
	xpathTemplate TEXT NOT NULL,
	PRIMARY KEY(id, schema_set)
);
