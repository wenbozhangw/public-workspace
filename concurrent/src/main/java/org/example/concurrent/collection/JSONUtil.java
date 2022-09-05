package org.example.concurrent.collection;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

/**
 * @author wenbo.zhang
 * @date 2022/7/27 14:26
 */
public class JSONUtil {

    static String json = "{\"policyUuid\":\"8d4f2661ffa643f2984d317d360e0fcb\",\"policyCode\":\"lfytest\",\"policyVersion\":4,\"policyMode\":2,\"businessType\":1,\"productCode\":\"02080001\",\"appCode\":\"test_app\",\"orgCode\":\"TongDun\",\"bizModel\":1,\"eventType\":\"Loan\",\"tokenId\":\"165890105548306A762181914378\",\"bizId\":\"BO202206003\",\"bizTime\":1658901055552,\"callTime\":1658901055552,\"invokeType\":1,\"requestParams\":{\"S_S_APPCODE\":\"test_app\",\"S_S_ORGCODE\":\"TongDun\",\"S_E_CUSTTYPE\":\"1\",\"S_S_BIZID\":\"BO202206003\",\"S_S_CUSTNO\":\"BO202206003\",\"S_E_RUNTYPE\":\"2\",\"S_S_POLICYCODE\":\"lfytest\",\"S_S_PRODUCTCODE\":\"02080001\"},\"searchFields\":[{\"name\":\"S_S_CUSTNO\",\"value\":\"BO202206003\"},{\"name\":\"S_S_PRODUCTCODE\",\"value\":\"02080001\"}],\"contextFields\":{\"S_E_CUSTTYPE\":\"1\",\"S_S_CUSTNO\":\"BO202206003\",\"serviceCode\":\"lfytest\",\"ipReputation\":\"{}\",\"S_S_PRODUCTCODE\":\"02080001\",\"S_S_APPCODE\":\"test_app\",\"geo\":\"{\\\"tokenId\\\":\\\"165890105548306A762181914378\\\"}\",\"S_S_ORGCODE\":\"TongDun\",\"S_S_BIZID\":\"BO202206003\",\"S_E_RUNTYPE\":\"2\",\"S_S_POLICYCODE\":\"lfytest\",\"browserInfo\":\"{\\\"tokenId\\\":\\\"165890105548306A762181914378\\\"}\"},\"outputFields\":{\"S_S_BIZID\":\"BO202206003\"},\"ruleDetails\":{},\"nodeDataList\":[],\"nodeResultList\":[{\"success\":true,\"nodeId\":\"9d68c180a9ac11ec8e1f05175a471eaf\",\"nodeName\":\"开始\",\"nodeType\":\"StartFlowNode\",\"outgoingNodeId\":\"a0b1aaf0a9ac11ec8e1f05175a471eaf\",\"incomingFields\":{},\"outgoingFields\":{},\"time\":1658901055556,\"nodeCost\":0,\"order\":1,\"stepOrder\":1},{\"success\":false,\"nodeId\":\"a0b1aaf0a9ac11ec8e1f05175a471eaf\",\"nodeName\":\"lfytest\",\"nodeType\":\"RuleSetServiceNode\",\"incomingLineId\":\"9d68c180a9ac11ec8e1f05175a471eaf.1=a0b1aaf0a9ac11ec8e1f05175a471eaf.3\",\"outgoingNodeId\":\"a56dfa80a9ac11ec8e1f05175a471eaf\",\"incomingFields\":{},\"outgoingFields\":{},\"message\":\"规则集加载有误\",\"time\":1658901055556,\"nodeCost\":1,\"order\":2,\"stepOrder\":2}],\"cost\":179,\"reasonCode\":501102,\"reasonMessage\":\"决策流执行失败\",\"reasonDetail\":\"[{\\\"errorMessage\\\":\\\"规则集加载有误\\\",\\\"nodeId\\\":\\\"a0b1aaf0a9ac11ec8e1f05175a471eaf\\\",\\\"nodeName\\\":\\\"lfytest\\\",\\\"nodeType\\\":\\\"RuleSetServiceNode\\\"}][50202-规则集加载有误]\",\"status\":3,\"msgId\":\"55658557490408924733096649191534161395587179813465\",\"serviceCode\":\"lfytest\",\"extensionParams\":{\"_ORIGIN_REQUEST_PARAMS\":[\"com.alibaba.fastjson.JSONObject\",{\"S_S_APPCODE\":\"test_app\",\"S_S_ORGCODE\":\"TongDun\",\"S_E_CUSTTYPE\":\"1\",\"S_S_BIZID\":\"BO202206003\",\"S_S_CUSTNO\":\"BO202206003\",\"S_E_RUNTYPE\":\"2\",\"S_S_POLICYCODE\":\"lfytest\",\"S_S_PRODUCTCODE\":\"02080001\"}],\"restoreFlowNodeId\":\"a0b1aaf0a9ac11ec8e1f05175a471eaf\",\"ExtensionRunType\":\"2\",\"serviceCode\":\"lfytest\",\"startTime\":[\"java.lang.Long\",1658901055552],\"_GUARD_ID\":\"guardId1342fbc0aa32a762181823e3547ff8285\"}}";

    public static void main(String[] args) {
        JSONArray reasonDetail = JSON.parseObject(json).getJSONArray("reasonDetail");
        System.out.println(reasonDetail
        );
    }
}
