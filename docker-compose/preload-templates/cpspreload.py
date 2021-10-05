import json
import time
import requests

CPS_HEADERS = {
   "Accept": "application/json",
   "Content-Type": "application/json",
   }
tbdmt_url="http://cps-tbdmt:8080/templates"
 


with open('samplepreload.json') as template_file:
  templates_json = template_file.read()
templates=json.loads(templates_json)


for template in templates:
    print(type(template))
    print("send rest req")
    print("waiting before sent req")
    time.sleep(2)
    response=requests.post(tbdmt_url, json=template, headers=CPS_HEADERS)
    print("res",response.reason)


