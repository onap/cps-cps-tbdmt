CREATE TABLE Template(
	id TEXT NOT NULL,
	model TEXT NOT NULL,
	xpath_template TEXT NOT NULL,
	PRIMARY KEY(id, schema_set)
);
