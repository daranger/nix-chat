-- Only a keyed hash of the phone number is stored, never the number itself.
ALTER TABLE users ADD COLUMN phone_hash VARCHAR(64);
CREATE UNIQUE INDEX users_phone_hash_uq ON users (phone_hash);
