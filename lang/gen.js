const fs=require("fs");
var startTime=new Date().getTime(),langList=[];

if(!fs.existsSync("./output")){
    fs.mkdirSync("./output");
}

function doConvert(file){
    var json={};
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
    return json;
}

fs.readdirSync("./texts").forEach(function(file){
    //check if language file
    if(file.endsWith(".lang")){
        var result=doConvert(fs.readFileSync("./texts/"+file).toString())
                ,fileName=file.replace(/lang/g,"json").toLowerCase();
        fs.writeFileSync("./output/"+fileName,JSON.stringify(result));
        langList.push(fileName);
        console.log("Converting "+file);
    }
})
fs.writeFileSync("./output/list.json",JSON.stringify(langList))

console.log("Convert complete("+(new Date().getTime()-startTime)+"ms)")