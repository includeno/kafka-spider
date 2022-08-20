import pymysql

def localdb():
    # 打开数据库连接
    db = pymysql.connect(host='127.0.0.1',
                        user='root',
                        password='root123456',
                        database='backup')

    # 使用 cursor() 方法创建一个游标对象 cursor
    cursor = db.cursor()
    return cursor

cursor=localdb()
# 使用 execute()  方法执行 SQL 查询 
cursor.execute("SELECT url FROM backup.spider_record where valid=1 group by url")
# 使用 fetchone() 方法获取单条数据.
data = cursor.fetchall()
print(len(list(data)))
dbdata=[]
for item in list(data):
    item=item[0]
    dbdata.append(item)

import os
urlfile=os.path.dirname(__file__)+"/data.txt"

def write_file(name,data):
    infile=open(name,mode="w",encoding="utf-8")
    infile.writelines(data)
    infile.close()

def read_file(name):
    infile=open(name,mode="r",encoding="utf-8")
    ans=infile.readlines()
    infile.close()
    return ans

import requests
#submit spider
ip="localhost"
host="http://"+str(ip).replace(" ","")+":8080/filter"
res=requests.post(str(host),data={"list":list(dbdata)})
print(len(list(res)))
print(eval(res.content))

print(len(eval(res.content)))

filedata=[]
for item in eval(res.content):
    filedata.append(item+"\n")

write_file(urlfile,filedata)