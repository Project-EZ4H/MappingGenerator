const fs=require("fs");
var itemJson=JSON.parse(fs.readFileSync("./items.json").toString()),outputJson={}; 
var keys=Object.keys(itemJson);
for(var i=0;i<keys.length;i++){
    var name=keys[i].split(":")[1],json=itemJson[keys[i]];
    outputJson[json.bedrock_id+":"+json.bedrock_data]=name;
}
fs.writeFileSync("bedrock_items.json",JSON.stringify(outputJson))