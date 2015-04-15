<#macro dateFormat date>${date?string("dd MMM yyyy HH:mm:ss 'GMT'Z '('zzz')'")}</#macro>
<#escape x as jsonUtils.encodeJSONString(x)>
{
   <#if results??>
   "numResults": ${results?size?c},
   "results": [
   <#list results as result>
   	<#assign qnamePath=result.qnamePath />
      {
		   "nodeRef": "${result.nodeRef}",
		   "qnamePath": {
		   	"name": "${result.qnamePath}",
		   	"prefixedName": "${result.prefixedQNamePath}"
			},
         "parentNodeRef": "<#if result.parent??>${result.parent.nodeRef}</#if>",
         "lastPushToIndex": <#if result.lastPushToIndex??>"<@dateFormat result.lastPushToIndex />"<#else>null</#if>,
         "lastPushFailed": <#if result.lastPushFailed??>${result.lastPushFailed?string}<#else>false</#if>,
         "inIndex": <#if result.inIndex??>${result.inIndex?string}<#else>false</#if>,
         "name": "<#if result.name??>${result.name}</#if>",
         "path": "<#if result.path??>${result.path}</#if>",
         "siteShortName": "<#if result.siteShortName??>${result.siteShortName}</#if>",
         "siteName": "<#if result.siteName??>${result.siteName}</#if>"
      }<#if result_has_next>,</#if>
   </#list>
   ]
   </#if>
}
</#escape>