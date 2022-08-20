import requests
import os

urlfile=os.path.dirname(__file__)+"/data.txt"
def read_file(name):
    infile=open(name,mode="r",encoding="utf-8")
    ans=infile.readlines()
    infile.close()
    return ans

ans=read_file(urlfile)

data=[]
length=(int)(len(ans))
for i in range(length):
    if(i<length):
        data.append(str(ans[i]).replace("\n",""))

print(len(data))

#submit spider
ip="localhost"
host="http://"+str(ip).replace(" ","")+":8080/sql/multi/batch"
res=requests.post(str(host),data={"list":list(data)})
print(res)