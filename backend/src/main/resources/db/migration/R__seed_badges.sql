INSERT INTO badges (code, name, description, icon_url) VALUES
    ('ORIGIN', 'Origin', 'One of the first members of Critiqal',
    ('CENTURION', 'Centurion', '100 days on Critiqal',
    ('GLADIATOR', 'Gladiator', 'One year of active presence on Critiqal',
    ('LEGATUS', 'Legatus', 'Two years on Critiqal',
    ('SCRIBE', 'Scribe', 'Published first post',
    ('ORATOR', 'Orator', 'Published 50 posts',
    ('TRIBUNE', 'TRIBUNE', 'Active contributor to discussions',
ON CONFLICT (code) DO UPDATE
    SET name = EXCLUDED.name,
         description = EXCLUDED.description;