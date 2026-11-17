-- Un color puede ser de dos tonos (ej. amarillo y blanco): '#RRGGBB' o '#RRGGBB/#RRGGBB'.
ALTER TABLE colors ALTER COLUMN hex TYPE VARCHAR(16);
