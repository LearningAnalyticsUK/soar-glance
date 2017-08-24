1. Create Postgres database with correct details
2. Run flyWay Migrate through sbt
3. Download datafiles
4. Run transform job
5. Run generate job
6. Run load-support job
7. Manually execute .sql dump in surveys-bin against database
8. Start sbt console
9. Run glance-evalJVM/reStart

## Updating (no data changes)
1. Pull down repo
2. run flyWayMigrate  
3. Perform steps 8-9.

## Updating (data changes)
1. Pull down repo
2. Backup database with pg_dump or similar
3. flywayClean && flywayMigrate
4. Rerun steps 4-9 above. 


