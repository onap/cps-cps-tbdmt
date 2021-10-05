import json
import time
import requests

CPS_HEADERS = {
   "Accept": "application/json",
   "Content-Type": "application/json",
   }
tbdmt_url="http://10.31.4.14:8088/templates"
  


with open('samplepreload.json') as ff:
  valobj = ff.read()
jsonobj=json.loads(valobj)


for x in range(0,len(jsonobj)):
    print(type(jsonobj[x]))
    print("send rest req")
    print("waiting before sent req")
    time.sleep(2)
    response=requests.post(tbdmt_url, json=jsonobj[x], headers=CPS_HEADERS)
    print("res",response.reason)


