/**
 * RL root namespace.
 *
 * @namespace RL
 */
// Ensure RL root object exists
if (typeof RL == "undefined" || !RL) {
   var RL = {};
}

/**
 * Admin Console Findwise Service Status
 *
 * @namespace Alfresco
 * @class RL.FindwiseIndexManager
 */
(function() {
   /**
    * YUI Library aliases
    */
   var Dom = YAHOO.util.Dom,
       Event = YAHOO.util.Event,
       Element = YAHOO.util.Element;
   
   /**
    * Alfresco Slingshot aliases
    */
   var $html = Alfresco.util.encodeHTML;

   /**
    * FindwiseIndexManager constructor.
    *
    * @param {String}
    *           htmlId The HTML id of the parent element
    * @return {RL.FindwiseIndexManager} The new FindwiseIndexManager instance
    * @constructor
    */
   RL.FindwiseIndexManager = function(htmlId) {
      this.name = "RL.FindwiseIndexManager";
      RL.FindwiseIndexManager.superclass.constructor.call(this, htmlId);

      /* Register this component */
      Alfresco.util.ComponentManager.register(this);

      /* Load YUI Components */
      Alfresco.util.YUILoaderHelper.require([ "button", "container", "datasource", "datatable", "paginator", "json", "history" ], this.onComponentsLoaded, this);

      /* Define panel handlers */
      var parent = this;

      /* File List Panel Handler */
      ListPanelHandler = function ListPanelHandler_constructor() {
         ListPanelHandler.superclass.constructor.call(this, "main");
      };

      YAHOO.extend(ListPanelHandler, Alfresco.ConsolePanelHandler, {
         /**
          * Called by the ConsolePanelHandler when this panel shall be loaded
          *
          * @method onLoad
          */
        onLoad : function onLoad() {
          // Buttons
          parent.widgets.reindexButton = Alfresco.util.createYUIButton(parent, "reindex-button", parent.onReindexClick);
          parent.widgets.reindexEverythingButton = Alfresco.util.createYUIButton(parent, "reindexEverything-button", parent.onReindexEverythingClick);
        }
      });

      new ListPanelHandler();

      return this;
   };

   YAHOO.extend(RL.FindwiseIndexManager, Alfresco.ConsoleTool, {
      

      /**
       * Fired by YUI when parent element is available for scripting. Component initialisation, including instantiation of YUI widgets and event listener binding.
       *
       * @method onReady
       */
      onReady : function FindwiseIndexManager_onReady() {
        var me = this;
        // Call super-class onReady() method
        RL.FindwiseIndexManager.superclass.onReady.call(this);

      },      

      onReindexClick : function FindwiseIndexManager_onReindexClick() {
        var self = this;
        var nodeRef = Dom.get(this.id+"-reindexNode").value.replace("://","/");
        if (nodeRef.indexOf("workspace")===0 && nodeRef.indexOf("SpacesStore")===10) {
          var url = YAHOO.lang.substitute(Alfresco.constants.PROXY_URI + "findwise/node/reindex/" + "{nodeRef}",
          {
            nodeRef: nodeRef,
          });

          // The data object, which contains the needed information
          var dataObj =
          {
            
          };

          Alfresco.util.Ajax.jsonPost(
          {
            url: url,
            dataObj: dataObj,
            successCallback:
            {
              fn: function FindwiseIndexManager_onReindexClick_success(response)
              {
                if (response && response.json && response.json.success && response.json.success === true) {
                  Alfresco.util.PopupManager.displayMessage(
                  {
                     text: this.msg("message.reindex.success"),
                     displayTime: 0.5
                  });
                } else {
                  Alfresco.util.PopupManager.displayMessage(
                  {
                    text: this.msg("message.reindex.failure")
                  });
                }
              },
              scope: this
            },
            failureCallback:
            {
              fn: function FindwiseIndexManager_onReindexClick_failure(response)
              {
                Alfresco.util.PopupManager.displayMessage(
                {
                  text: this.msg("message.reindex.failure")
                });
              },
              scope: this
            }
          });
        } else {
          Alfresco.util.PopupManager.displayMessage(
          {
            text: this.msg("message.invalidNodeRef")
          });
        }
      },

      onReindexEverythingClick : function FindwiseIndexManager_onReindexClick() {
        var self = this;
        var password = Dom.get(this.id+"-reindexEverything").value;
        if (password.indexOf("reindexEverything")===0) {
          Dom.get(this.id+"-reindexEverything").value = "";
          var url = YAHOO.lang.substitute(Alfresco.constants.PROXY_URI + "findwise/node/reindex/" + "{nodeRef}",
          {
            nodeRef: "everything",
          });

          // The data object, which contains the needed information
          var dataObj =
          {
            
          };

          Alfresco.util.Ajax.jsonPost(
          {
            url: url,
            dataObj: dataObj,
            successCallback:
            {
              fn: function FindwiseIndexManager_onReindexClick_success(response)
              {
                if (response && response.json && response.json.success && response.json.success === true) {
                  Alfresco.util.PopupManager.displayMessage(
                  {
                     text: this.msg("message.reindex.success"),
                     displayTime: 0.5
                  });
                } else {
                  Alfresco.util.PopupManager.displayMessage(
                  {
                    text: this.msg("message.reindex.failure")
                  });
                }
              },
              scope: this
            },
            failureCallback:
            {
              fn: function FindwiseIndexManager_onReindexClick_failure(response)
              {
                Alfresco.util.PopupManager.displayMessage(
                {
                  text: this.msg("message.reindex.failure")
                });
              },
              scope: this
            }
          });
        } else {
          Alfresco.util.PopupManager.displayMessage(
          {
            text: this.msg("label.reindexEverythingMessage")
          });
        }
      },

      _msg: function FindwiseIndexManager_msg(messageId)
      {
        return Alfresco.util.message.call(this, messageId, "RL.FindwiseIndexManager", Array.prototype.slice.call(arguments).slice(1));
      }


  });
})();
