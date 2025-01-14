--
-- Copyright (C) 2019 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

INSERT INTO Deposit (depositId, bagName, creationTimestamp, depositorId)
VALUES ('00000000-0000-0000-0000-000000000001', 'bag1', '2019-01-01 00:00:00', 'user001'),
       ('00000000-0000-0000-0000-000000000002',  null , '2019-02-02 00:00:00', 'user001'),
       ('00000000-0000-0000-0000-000000000003', 'bag3', '2019-03-03 00:00:00', 'user002'),
       ('00000000-0000-0000-0000-000000000004', 'bag4', '2019-04-04 00:00:00', 'user001'),
       ('00000000-0000-0000-0000-000000000005', 'bag5', '2019-05-05 00:00:00', 'user002');

INSERT INTO State (depositId, label, description, timestamp)
VALUES ('00000000-0000-0000-0000-000000000001', 'DRAFT'     , 'draft with continued deposit'     , '2019-01-01 00:00:00'),
       ('00000000-0000-0000-0000-000000000001', 'DRAFT'     , 'draft with continued deposit'     , '2019-01-01 01:01:00'),
       ('00000000-0000-0000-0000-000000000001', 'UPLOADED'  , 'deposit upload has been completed', '2019-01-01 02:02:00'),
       ('00000000-0000-0000-0000-000000000001', 'FINALIZING', 'deposit is finalizing'            , '2019-01-01 03:03:00'),
       ('00000000-0000-0000-0000-000000000001', 'SUBMITTED' , 'deposit is processing'            , '2019-01-01 04:04:00'),
       ('00000000-0000-0000-0000-000000000001', 'ARCHIVED'  , 'deposit is archived'              , '2019-01-01 05:05:00'),
       ('00000000-0000-0000-0000-000000000002', 'UPLOADED'  , 'deposit upload has been completed', '2019-02-02 00:00:00'),
       ('00000000-0000-0000-0000-000000000002', 'FINALIZING', 'deposit is finalizing'            , '2019-02-02 01:01:00'),
       ('00000000-0000-0000-0000-000000000002', 'SUBMITTED' , 'deposit is processing'            , '2019-02-02 02:02:00'),
       ('00000000-0000-0000-0000-000000000002', 'ARCHIVED'  , 'deposit is archived'              , '2019-02-02 03:03:00'),
       ('00000000-0000-0000-0000-000000000003', 'UPLOADED'  , 'deposit upload has been completed', '2019-03-03 00:00:00'),
       ('00000000-0000-0000-0000-000000000003', 'FINALIZING', 'deposit is finalizing'            , '2019-03-03 01:01:00'),
       ('00000000-0000-0000-0000-000000000003', 'INVALID'   , 'deposit is invalid'               , '2019-03-03 02:02:00'),
       ('00000000-0000-0000-0000-000000000004', 'UPLOADED'  , 'deposit upload has been completed', '2019-04-04 00:00:00'),
       ('00000000-0000-0000-0000-000000000004', 'FINALIZING', 'deposit is finalizing'            , '2019-04-04 01:01:00'),
       ('00000000-0000-0000-0000-000000000004', 'ARCHIVED'  , 'deposit is archived'              , '2019-04-04 02:02:00'),
       ('00000000-0000-0000-0000-000000000005', 'UPLOADED'  , 'deposit upload has been completed', '2019-05-05 00:00:00'),
       ('00000000-0000-0000-0000-000000000005', 'FINALIZING', 'deposit is finalizing'            , '2019-05-05 01:01:00'),
       ('00000000-0000-0000-0000-000000000005', 'SUBMITTED' , 'deposit is processing'            , '2019-05-05 02:02:00'),
       ('00000000-0000-0000-0000-000000000005', 'REJECTED'  , 'deposit is rejected'              , '2019-05-05 03:03:00');

INSERT INTO Identifier (depositId, identifierSchema, identifierValue, timestamp)
VALUES ('00000000-0000-0000-0000-000000000001', 'bag-store', '00000000-0000-0000-0000-000000000001', '2019-01-01 00:00:00'),
       ('00000000-0000-0000-0000-000000000001', 'doi'      , '10.5072/dans-a1b-cde2'               , '2019-01-01 00:01:00'),
       ('00000000-0000-0000-0000-000000000001', 'urn'      , 'urn:nbn:123456'                      , '2019-01-01 00:02:00'),
       ('00000000-0000-0000-0000-000000000001', 'fedora'   , 'easy-dataset:1'                      , '2019-01-01 00:03:00'),
       ('00000000-0000-0000-0000-000000000002', 'bag-store', '00000000-0000-0000-0000-000000000002', '2019-02-02 00:00:00'),
       ('00000000-0000-0000-0000-000000000002', 'doi'      , '10.5072/dans-f3g-hij4'               , '2019-02-02 00:01:00'),
       ('00000000-0000-0000-0000-000000000002', 'urn'      , 'urn:nbn:789012'                      , '2019-02-02 00:02:00'),
       ('00000000-0000-0000-0000-000000000002', 'fedora'   , 'easy-dataset:2'                      , '2019-02-02 00:03:00'),
       ('00000000-0000-0000-0000-000000000003', 'bag-store', '00000000-0000-0000-0000-000000000003', '2019-03-03 00:00:00'),
       ('00000000-0000-0000-0000-000000000004', 'bag-store', '00000000-0000-0000-0000-000000000004', '2019-04-04 00:00:00'),
       ('00000000-0000-0000-0000-000000000004', 'doi'      , '10.5072/dans-p7q-rst8'               , '2019-04-04 00:01:00'),
       ('00000000-0000-0000-0000-000000000004', 'urn'      , 'urn:nbn:901234'                      , '2019-04-04 00:02:00'),
       ('00000000-0000-0000-0000-000000000004', 'fedora'   , 'easy-dataset:4'                      , '2019-04-04 00:03:00'),
       ('00000000-0000-0000-0000-000000000005', 'bag-store', '00000000-0000-0000-0000-000000000005', '2019-05-05 00:00:00');

INSERT INTO Curation (depositId, isNewVersion, isRequired, isPerformed, datamanagerUserId, datamanagerEmail, timestamp)
VALUES ('00000000-0000-0000-0000-000000000001', null   , 'true', 'false', 'archie002', 'does.not.exists2@dans.knaw.nl', '2019-01-01 00:00:00'),
       ('00000000-0000-0000-0000-000000000001', null   , 'true', 'false', 'archie001', 'does.not.exists1@dans.knaw.nl', '2019-01-01 03:03:00'),
       ('00000000-0000-0000-0000-000000000001', null   , 'true', 'true' , 'archie001', 'does.not.exists1@dans.knaw.nl', '2019-01-01 04:04:00'),
       ('00000000-0000-0000-0000-000000000003', null   , 'true', 'false', 'archie001', 'does.not.exists1@dans.knaw.nl', '2019-03-03 00:00:00'),
       ('00000000-0000-0000-0000-000000000003', null   , 'true', 'false', 'archie002', 'does.not.exists2@dans.knaw.nl', '2019-03-03 04:04:00'),
       ('00000000-0000-0000-0000-000000000003', 'true' , 'true', 'true' , 'archie002', 'does.not.exists2@dans.knaw.nl', '2019-03-03 06:06:00'),
       ('00000000-0000-0000-0000-000000000004', 'false', 'true', 'false', 'archie001', 'does.not.exists1@dans.knaw.nl', '2019-04-04 00:00:00'),
       ('00000000-0000-0000-0000-000000000004', 'false', 'true', 'true' , 'archie001', 'does.not.exists1@dans.knaw.nl', '2019-04-04 04:04:00'),
       ('00000000-0000-0000-0000-000000000005', 'false', 'true', 'false', 'archie001', 'does.not.exists1@dans.knaw.nl', '2019-05-05 00:00:00'),
       ('00000000-0000-0000-0000-000000000005', 'false', 'true', 'true' , 'archie001', 'does.not.exists1@dans.knaw.nl', '2019-05-05 04:04:00');

INSERT INTO Springfield (depositId, domain, springfield_user, collection, playmode, timestamp)
VALUES ('00000000-0000-0000-0000-000000000001', 'domain1', 'user1', 'collection1', 'continuous', '2019-01-01 00:00:00'),
       ('00000000-0000-0000-0000-000000000002', 'domain1', 'user1', 'collection1', 'continuous', '2019-02-02 00:00:00'),
       ('00000000-0000-0000-0000-000000000002', 'domain2', 'user2', 'collection2', 'menu'      , '2019-02-02 02:02:00');

INSERT INTO SimpleProperties (depositId, key, value, timestamp)
VALUES ('00000000-0000-0000-0000-000000000001', 'ingest-step', 'VALIDATE'     , '2019-01-01 04:05:00'),
       ('00000000-0000-0000-0000-000000000001', 'ingest-step', 'PID_GENERATOR', '2019-01-01 04:06:00'),
       ('00000000-0000-0000-0000-000000000001', 'ingest-step', 'FEDORA'       , '2019-01-01 04:07:00'),
       ('00000000-0000-0000-0000-000000000001', 'ingest-step', 'SPRINGFIELD'  , '2019-01-01 04:08:00'),
       ('00000000-0000-0000-0000-000000000001', 'ingest-step', 'BAGSTORE'     , '2019-01-01 04:09:00'),
       ('00000000-0000-0000-0000-000000000001', 'ingest-step', 'SOLR4FILES'   , '2019-01-01 04:10:00'),
       ('00000000-0000-0000-0000-000000000001', 'ingest-step', 'COMPLETED'    , '2019-01-01 04:11:00'),
       ('00000000-0000-0000-0000-000000000002', 'ingest-step', 'VALIDATE'     , '2019-02-02 02:05:00'),
       ('00000000-0000-0000-0000-000000000002', 'ingest-step', 'PID_GENERATOR', '2019-02-02 02:06:00'),
       ('00000000-0000-0000-0000-000000000002', 'ingest-step', 'FEDORA'       , '2019-02-02 02:07:00'),
       ('00000000-0000-0000-0000-000000000002', 'ingest-step', 'SPRINGFIELD'  , '2019-02-02 02:08:00'),
       ('00000000-0000-0000-0000-000000000002', 'ingest-step', 'BAGSTORE'     , '2019-02-02 02:09:00'),
       ('00000000-0000-0000-0000-000000000002', 'ingest-step', 'SOLR4FILES'   , '2019-02-02 02:10:00'),
       ('00000000-0000-0000-0000-000000000002', 'ingest-step', 'COMPLETED'    , '2019-02-02 02:11:00'),
       ('00000000-0000-0000-0000-000000000005', 'ingest-step', 'VALIDATE'     , '2019-05-05 04:05:00'),

       ('00000000-0000-0000-0000-000000000001', 'doi-registered', 'false', '2019-01-01 00:00:00'),
       ('00000000-0000-0000-0000-000000000001', 'doi-registered', 'true' , '2019-01-01 04:07:00'),
       ('00000000-0000-0000-0000-000000000002', 'doi-registered', 'false', '2019-02-02 00:00:00'),
       ('00000000-0000-0000-0000-000000000002', 'doi-registered', 'true' , '2019-02-02 02:07:00'),
       ('00000000-0000-0000-0000-000000000005', 'doi-registered', 'false', '2019-05-05 00:00:00'),

       ('00000000-0000-0000-0000-000000000001', 'doi-action', 'update', '2019-01-01 01:01:00'),
       ('00000000-0000-0000-0000-000000000001', 'doi-action', 'none'  , '2019-01-01 04:05:00'),
       ('00000000-0000-0000-0000-000000000002', 'doi-action', 'create', '2019-02-02 00:00:00'),
       ('00000000-0000-0000-0000-000000000003', 'doi-action', 'create', '2019-03-03 00:00:00'),
       ('00000000-0000-0000-0000-000000000004', 'doi-action', 'create', '2019-04-04 00:00:00'),
       ('00000000-0000-0000-0000-000000000005', 'doi-action', 'update', '2019-05-05 00:00:00'),

       ('00000000-0000-0000-0000-000000000001', 'content-type', 'application/zip'         , '2019-01-01 00:05:00'),
       ('00000000-0000-0000-0000-000000000001', 'content-type', 'application/octet-stream', '2019-01-01 00:10:00'),
       ('00000000-0000-0000-0000-000000000002', 'content-type', 'application/zip'         , '2019-02-02 00:05:00'),
       ('00000000-0000-0000-0000-000000000003', 'content-type', 'application/zip'         , '2019-03-03 00:05:00'),
       ('00000000-0000-0000-0000-000000000004', 'content-type', 'application/zip'         , '2019-04-04 00:05:00'),
       ('00000000-0000-0000-0000-000000000005', 'content-type', 'application/zip'         , '2019-05-05 00:05:00');
