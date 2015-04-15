<#compress>

<#assign el=args.htmlid?html>

<#assign el=args.htmlid?html>
<script type="text/javascript">//<![CDATA[
  new RL.FindwiseIndexManager("${el}").setMessages(${messages});
//]]></script>

<div id="${el}-body" class="index-manager-console">
  <div id="${el}-main" class="hidden">
    <div>
      <h1 class="thin dark">${msg("label.findwise.index-manager")}</h1>
      <div id="${el}-body" class="node-browser">
        <!-- Search panel -->
        <div id="${el}-search">
          <div class="yui-g separator">
            <div class="yui-u first">
              <div class="title"><label for="${el}-reindexNode">${msg("label.reindexNode")}</label></div>
            </div>
          </div>
          <div class="yui-g separator">
            <div class="search-text">
              <!-- Search field -->
              <input type="text" id="${el}-reindexNode" name="-" value="" />
              <!-- Search button -->
              <span class="yui-button yui-push-button" id="${el}-reindex-button">
                <span class="first-child"><button>${msg("button.reindex")}</button></span>
              </span>
            </div>
          </div>    

          <div class="yui-g separator">
            <div class="yui-u first">
              <div class="title"><label for="${el}-reindexEverything">${msg("label.reindexEverything")}</label></div>
              <div class="text">${msg("label.reindexEverythingMessage")}</div>
            </div>
          </div>
          <div class="yui-g separator">
            <div class="search-text">
              <!-- Search field -->
              <input type="text" id="${el}-reindexEverything" name="-" value="" />
              <!-- Search button -->
              <span class="yui-button yui-push-button" id="${el}-reindexEverything-button">
                <span class="first-child"><button>${msg("button.reindexEverything")}</button></span>
              </span>
            </div>
          </div>            
        </div>
      </div>  
    </div>
  </div>
</div>

</#compress>