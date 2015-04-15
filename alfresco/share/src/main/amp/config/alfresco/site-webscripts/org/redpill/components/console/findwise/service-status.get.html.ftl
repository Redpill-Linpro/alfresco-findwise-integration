<#compress>

<#assign el=args.htmlid?html>

<#assign el=args.htmlid?html>
<script type="text/javascript">//<![CDATA[
  new RL.FindwiseServiceStatus("${el}").setMessages(${messages});
//]]></script>

<div id="${el}-body" class="index-manager-console">
  <div id="${el}-main" class="hidden">
    <div>
      <h1 class="thin dark">${msg("label.findwise.service-status")}</h1>
      <div id="${el}-body" class="node-browser">
        <!-- Search panel -->
        <div id="${el}-search">
          <div class="yui-g separator">
          <div class="search-text">
             <!-- Search button -->
             <span class="yui-button yui-push-button" id="${el}-refresh-button">
                <span class="first-child"><button>${msg("button.refresh")}</button></span>
             </span>
          </div>
          </div>
          <div class="search-main">
             <div id="${el}-search-bar" class="search-bar theme-bg-color-3">${msg("message.noresults")}</div>
             <div class="results" id="${el}-datatable"></div>
          </div>
        </div>
      </div>  
    </div>
  </div>
</div>

</#compress>