INSERT INTO badges (code, name, description, icon_url) VALUES
    ('ORIGIN',    'Origin',    'One of the first members of Critiqal',               null),
    ('CENTURION', 'Centurion', '100 days on Critiqal',                               null),
    ('GLADIATOR', 'Gladiator', 'One year of active presence on Critiqal',            null),
    ('LEGATUS',   'Legatus',   'Two years on Critiqal',                              null),
    ('SCRIBE',    'Scribe',    'Published first post',                               null),
    ('ORATOR',    'Orator',    'Published 50 posts',                                 null),
    ('TRIBUNE',   'Tribune',   'Active contributor to discussions',                  null)
ON CONFLICT (code) DO UPDATE
    SET name        = EXCLUDED.name,
        description = EXCLUDED.description;