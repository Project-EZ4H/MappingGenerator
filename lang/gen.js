const fs=require("fs") 
var file=fs.readFileSync("en_US.lang").toString(),json={};
for(var i=1;i<7;i++){
    file=file.replaceAll("%"+i+"$s","%"+i).replaceAll("%"+i+"$d","%"+i)
}
file=file.split("\n")
for(var i=0;i<file.length;i++){
    if(file[i].indexOf('=')!=-1){
        var str=file[i].replaceAll('    ',"").replaceAll('\t','').replaceAll('\r',"");
        if(str.indexOf("#")!=-1){
            str=str.split('#')[0];
        }
        str=str.split('=')
        json[str[0]]=str[1];
    }
}
fs.writeFileSync("lang.json",JSON.stringify(json))