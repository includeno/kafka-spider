import os

urlfile=os.path.dirname(__file__)+"/data.txt"
def read_file(name):
    infile=open(name,mode="r",encoding="utf-8")
    ans=infile.readlines()
    infile.close()
    return ans

def write_file(name,data):
    infile=open(name,mode="w",encoding="utf-8")
    infile.writelines(data)
    infile.close()

ans=read_file(urlfile)

result=[]
for line in ans:
    
    if(str(line).count("https://")>1):
        for item in str(line).split("https://"):
            if(item!=''):
                result.append("https://"+str(item).replace("\n","").split("?")[0]+"\n")
    else:
        result.append(str(line).replace("\n","").split("?")[0]+"\n")

write_file(urlfile,result)