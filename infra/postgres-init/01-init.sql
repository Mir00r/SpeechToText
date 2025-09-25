-- Create database and user if they don't exist
-- This script runs when PostgreSQL container starts

-- Create database
SELECT 'CREATE DATABASE speechtotext'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'speechtotext')\gexec

-- Note: User creation is handled by docker environment variables