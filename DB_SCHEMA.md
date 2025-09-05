# DB Schema Notes
- Entities: Project, Settings, UserAccess, MappingDef, Job, JobError, Settings
- MappingDef.mappingJson stores the mapping designer JSON
- To seed an admin: insert into user_access (email, role_name) values ('you@domain.com','ADMIN');
