import pymysql

def localdb():
    # 打开数据库连接
    db = pymysql.connect(host='127.0.0.1',
                        user='root',
                        password='root123456',
                        database='demo2')

    # 使用 cursor() 方法创建一个游标对象 cursor
    cursor = db.cursor()
    return cursor
def aliyun():
    # 打开数据库连接
    db = pymysql.connect(host='rm-uf6oq09op75610yz6lo.mysql.rds.aliyuncs.com',
                        user='root',
                        password='E3*Ziv$FF9gbQZ$',
                        database='demo')

    # 使用 cursor() 方法创建一个游标对象 cursor
    cursor = db.cursor()
    return cursor

cursor=localdb()
# 使用 execute()  方法执行 SQL 查询 
cursor.execute("SELECT url FROM demo2.spider_record group by url")
# 使用 fetchone() 方法获取单条数据.
data = cursor.fetchall()
print(len(list(data)))
temp=[]
for item in list(data):
    item=item[0]
    temp.append(item)
data=temp

cursor.execute("SELECT id FROM demo2.spider_record where content is null")
# 使用 fetchone() 方法获取单条数据.
errors = cursor.fetchall()
print("errors:"+str(len(list(errors))))

import os

urlfile=os.path.dirname(__file__)+"/data.txt"
def read_file(name):
    infile=open(name,mode="r",encoding="utf-8")
    ans=infile.readlines()
    infile.close()
    return ans

ans=read_file(urlfile)

inputdata=[]
length=len(ans)
for i in range(length):
    if(i<length):
        inputdata.append(str(ans[i]).replace("\n",""))

print(len(inputdata))

res=[]
inputset=set(inputdata)
for item in list(data):
    #print(item)
    if(item not in inputset):
        res.append(item)

#数据库中没有的
dbset=set(data)
for item in list(inputdata):
    #print(item)
    if(item not in dbset):
        res.append(item)

print(len(res))
for item in list(res):
    print(item+"\n")
