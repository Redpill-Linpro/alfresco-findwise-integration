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
 * @class RL.FindwiseServiceStatus
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
    * FindwiseServiceStatus constructor.
    *
    * @param {String}
    *           htmlId The HTML id of the parent element
    * @return {RL.FindwiseServiceStatus} The new FindwiseServiceStatus instance
    * @constructor
    */
   RL.FindwiseServiceStatus = function(htmlId) {
      this.name = "RL.FindwiseServiceStatus";
      RL.FindwiseServiceStatus.superclass.constructor.call(this, htmlId);

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
            parent.widgets.refreshButton = Alfresco.util.createYUIButton(parent, "refresh-button", parent.onRefreshClick);
         }
      });

      new ListPanelHandler();

      return this;
   };

   YAHOO.extend(RL.FindwiseServiceStatus, Alfresco.ConsoleTool, {
      /**
       * Name of the store to search against
       * 
       * @property store
       * @type string
       */
      store: "workspace://SpacesStore",
      
      /**
       * Current search term, obtained from form input field.
       * 
       * @property searchTerm
       * @type string
       */
      searchTerm: "ASPECT:\"fwi:findwiseIndexable\" AND @fwi:lastPushFailed:true",
      
      /**
       * Current search language, obtained from drop-down.
       * 
       * @property searchLanguage
       * @type string
       */
      searchLanguage: "fts-alfresco",

      /**
        * Maximum number of items to display in the results list
        * 
        * @property maxSearchResults
        * @type int
        * @default 100
        */
      maxSearchResults: 100,

      /**
       * Fired by YUI when parent element is available for scripting. Component initialisation, including instantiation of YUI widgets and event listener binding.
       *
       * @method onReady
       */
      onReady : function FindwiseServiceStatus_onReady() {
        var me = this;
        // Call super-class onReady() method
        RL.FindwiseServiceStatus.superclass.onReady.call(this);

        // DataTable and DataSource setup
        this.widgets.dataSource = new YAHOO.util.DataSource(Alfresco.constants.PROXY_URI + "findwise/node/search",
        {
           responseType: YAHOO.util.DataSource.TYPE_JSON,
           responseSchema:
           {
              resultsList: "results",
              metaFields:
              {
                 recordOffset: "startIndex",
                 totalRecords: "totalResults"
              }
           }
        });

        // Work to be performed after data has been queried but before display by the DataTable
        this.widgets.dataSource.doBeforeParseData = function FindwiseServiceStatus_doBeforeParseData(oRequest, oFullResponse)
        {
           var updatedResponse = oFullResponse;
           
           if (oFullResponse)
           {
              var items = oFullResponse.results;
              
              // initial sort by username field
              items.sort(function(a, b)
              {
                 return (a.name > b.name);
              });
              
              // we need to wrap the array inside a JSON object so the DataTable gets the object it expects
              updatedResponse =
              {
                 "results": items
              };
           }
           
           // update Results Bar message with number of results found
           if (items.length < me.maxSearchResults)
           {
              me._setResultsMessage("message.results", $html(me.searchTerm), items.length);
           }
           else
           {
              me._setResultsMessage("message.maxresults", me.maxSearchResults);
           }
           
           return updatedResponse;
        };
        
        // Setup the main datatable
        this._setupDataTable();

        this.onRefreshClick();
      },

      /**
        * Setup the YUI DataTable with custom renderers.
        *
        * @method _setupDataTable
        * @private
        */
      _setupDataTable: function FindwiseServiceStatus_setupDataTable()
      {
        var self = this;
        /**
         * DataTable Cell Renderers
         *
         * Each cell has a custom renderer defined as a custom function. See YUI documentation for details.
         * These MUST be inline in order to have access to the parent instance (via the "parent" variable).
         */
        
        /**
         * Generic HTML-safe custom datacell formatter
         */
        var renderCellSafeHTML = function renderCellSafeHTML(elCell, oRecord, oColumn, oData)
        {
          elCell.innerHTML = $html(oData);
        };

        /**
         * Qname renderer
         *
         * @method renderQName
         */
        var renderQName = function renderQName(elCell, oRecord, oColumn, oData)
        {
            elCell.innerHTML = $html(oData[self._qnamePropertyName()]);
        }

        /**
         * Node name renderer
         *
         * @method renderNodeName
         */
        var renderNodeName = function renderNodeName(elCell, oRecord, oColumn, oData)
        {
            if (oData != "")
            {
                renderNodeLink(elCell, oRecord, oColumn, $html(oData[self._qnamePropertyName()]));
            }
        }
        
        /**
         * Node name custom datacell formatter
         *
         * @method renderName
         */
        var renderNodeLink = function renderNodeLink(elCell, oRecord, oColumn, oData)
        {
           // Create view userlink
           var viewNodeLink = document.createElement("a");
           Dom.setAttribute(viewNodeLink, "href", "#");
           viewNodeLink.innerHTML = $html(oData);

           // fire the 'viewUserClick' event when the selected user in the list has changed
           YAHOO.util.Event.addListener(viewNodeLink, "click", function(e)
           {
              YAHOO.util.Event.preventDefault(e);
              YAHOO.Bubbling.fire('viewNodeClick',
              {
                 nodeRef: oRecord.getData("nodeRef")
              });
           }, null, parent);
           elCell.appendChild(viewNodeLink);
        };
        
        // DataTable column defintions
        var columnDefinitions =
        [
           { key: "name", label: this._msg("label.name"), sortable: true, formatter: renderCellSafeHTML },
           { key: "siteName", label: this._msg("label.siteName"), sortable: true, formatter: renderCellSafeHTML },
           { key: "path", label: this._msg("label.parent_path"), sortable: true, formatter: renderCellSafeHTML },           
           { key: "lastPushToIndex", label: this._msg("label.lastPushToIndex"), sortable: true, formatter: renderCellSafeHTML },
           { key: "lastPushFailed", label: this._msg("label.lastPushFailed"), sortable: true, formatter: renderCellSafeHTML },
           { key: "inIndex", label: this._msg("label.inIndex"), sortable: true, formatter: renderCellSafeHTML },
           { key: "nodeRef", label: this._msg("label.node-ref"), sortable: true, formatter: renderNodeLink }
        ];
        
        // DataTable definition
        this.widgets.dataTable = new YAHOO.widget.DataTable(this.id + "-datatable", columnDefinitions, this.widgets.dataSource,
        {
           initialLoad: false,
           renderLoopSize: 32,
           sortedBy:
           {
              key: "name",
              dir: "asc"
           },
           MSG_EMPTY: this._msg("message.empty")
        });
      },

      /**
      * Resets the YUI DataTable errors to our custom messages
      * NOTE: Scope could be YAHOO.widget.DataTable, so can't use "this"
      *
      * @method _setDefaultDataTableErrors
      * @param dataTable {object} Instance of the DataTable
      * @private
      */
      _setDefaultDataTableErrors: function FindwiseServiceStatus_setDefaultDataTableErrors(dataTable)
      {
        dataTable.set("MSG_EMPTY", this._msg("message.datatable.empty"));
        dataTable.set("MSG_ERROR", this._msg("message.datatable.error"));
      },

      successHandler: function FindwiseServiceStatus_onUpdate_successHandler(sRequest, oResponse, oPayload)
      {
        this._setDefaultDataTableErrors(this.widgets.dataTable);
        this.widgets.dataTable.onDataReturnInitializeTable.call(this.widgets.dataTable, sRequest, oResponse, oPayload);
      },

      failureHandler: function FindwiseServiceStatus_onUpdate_failureHandler(sRequest, oResponse)
      {
        if (oResponse.status == 401)
        {
           // Our session has likely timed-out, so refresh to offer the login page
           window.location.reload();
        }
        else
        {
           try
           {
              var response = YAHOO.lang.JSON.parse(oResponse.responseText);
              this.widgets.dataTable.set("MSG_ERROR", response.message);
              this.widgets.dataTable.showTableMessage(response.message, YAHOO.widget.DataTable.CLASS_ERROR);
              this._setResultsMessage("message.noresults");
           }
           catch(e)
           {
              this._setDefaultDataTableErrors(this.widgets.dataTable);
           }
        }
      },

      onRefreshClick : function FindwiseServiceStatus_onRefreshClick() {
        var self = this;

        // Send the query to the server
        this.widgets.dataSource.sendRequest(this._buildSearchParams(),
        {
          success: this.successHandler,
          failure: this.failureHandler,
          scope: self
        });
        this._setResultsMessage("message.searchingFor", $html(this.searchTerm));
      },

      /**
        * Build URI parameters for People List JSON data webscript
        *
        * @method _buildSearchParams
        * @param searchTerm {string} User search term
        * @param store {string} Store name
        * @private
        */
      _buildSearchParams: function FindwiseServiceStatus_buildSearchParams()
      {
        return "?q=" + encodeURIComponent(this.searchTerm) + 
          "&lang=" + encodeURIComponent(this.searchLanguage) + 
          "&store=" + encodeURIComponent(this.store) + 
          "&maxResults=" + this.maxSearchResults;
      },

      /**
        * Set the message in the Results Bar area
        * 
        * @method _setResultsMessage
        * @param messageId {string} The messageId to display
        * @private
        */
      _setResultsMessage: function FindwiseServiceStatus_setResultsMessage(messageId, arg1, arg2)
      {
        var resultsDiv = Dom.get(this.id + "-search-bar");
        resultsDiv.innerHTML = this._msg(messageId, arg1, arg2);
      },

      _qnamePropertyName: function FindwiseServiceStatus_qnamePropertyName()
      {
         return "prefixedName";
      },

      _msg: function FindwiseServiceStatus_msg(messageId)
      {
        return Alfresco.util.message.call(this, messageId, "RL.FindwiseServiceStatus", Array.prototype.slice.call(arguments).slice(1));
      }


  });
})();
