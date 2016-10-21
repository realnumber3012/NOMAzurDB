## NOMAzurDB ##

-- Demo: --
Required: sbt

Start rub.bat/run.sh

- create bucket with name "demobucket"
curl -XPUT http://localhost:14780/storage/demobucket

- add element to bucket
curl -XPOST http://localhost:14780/storage/demobucket/13

- test element in the bucket
curl -XGET http://localhost:14780/storage/demobucket/13
