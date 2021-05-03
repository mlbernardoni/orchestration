import uuid
import json
import requests
import time
import datetime

file = open('tests.csv', 'w')
rtoossend = {}
x = requests.post('http://localhost:8080/R2FileApp/Tests.html', json = rtoossend)
testsarray = json.loads(x.text)
for raw in testsarray:
    #print(raw)
    row = json.loads(raw)
    file_id = row['file_id']
    authenticate = row['authenticate']
    clear = row['clear']
    starttimestr = row['starttime']
    starttimestr = starttimestr.rstrip(starttimestr[-1])
    element = datetime.datetime.strptime(starttimestr, "%Y-%m-%d %H:%M:%S.%f")
    starttime = datetime.datetime.timestamp(element)
    endtimestr = row['endtime']
    endtimestr = endtimestr.rstrip(endtimestr[-1])
    element = datetime.datetime.strptime(endtimestr, "%Y-%m-%d %H:%M:%S.%f")
    endtime = datetime.datetime.timestamp(element)
    diff = endtime - starttime
    #seconds = diff.total_seconds()
    transactions = row['transactions']
    type = row['type']
    strcsv = file_id + ',' + type + ',' + authenticate + ',' + clear + ',' + str(transactions) + ',' + str(diff) + '\n'
    file.write(strcsv)
    #print(strcsv)

file.close()
