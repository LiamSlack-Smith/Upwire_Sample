(function (angular, module) {

    module.controller('TerminalFormController', function ($scope, $http, $q, $uibModalInstance, Terminals, predefinedFields, originalModel) {

        var self = this;
        var AUTO_TERMINAL_DESCRIPTION_PREFIX = 'Terminal Capacity ';
        var regAutoTerminalDescription = new RegExp('^\s*' + AUTO_TERMINAL_DESCRIPTION_PREFIX);


        this.init = function () {

            var form = $scope.form = $scope.masterForm = {};

            if (originalModel) {
                $scope.isUpdate = true;
                _.assign(form, _.cloneDeep(originalModel));
            } else {
                $scope.isUpdate = false;
                _.assign(form, {
                    symbol: undefined,
                    pathway: undefined,
                    originLocation: undefined,
                    originState: undefined,
                    productType: 'Crude',
                    restrictions: undefined,
                    showUser: true
                });
            }

            if (predefinedFields) {
                _.assign(form, predefinedFields);
            }

        };

        $scope.stateCodes = [
        ];

        $scope.continentCodes = [
            {label : 'Africa', value : 'AF'},
            {label : 'Antarctica', value : 'AN'},
            {label : 'Asia', value : 'AS'},
            {label : 'Europe', value : 'EU'},
            {label : 'North America', value : 'NA'},
            {label : 'Oceania', value : 'OC'},
            {label : 'South America', value : 'SA'}
        ];

        $scope.YesNoCodes = [
        ];

        $scope.transportModeCodes = [
            {label : 'Vessel', value : 'Vessel'},
            {label : 'Barge', value : 'Barge'},
            {label : 'Rail', value : 'Rail'},
            {label : 'Pipeline', value : 'Pipeline'},
            {label : 'Tank Truck', value : 'Tank Truck'}
        ];

        $scope.countryCodes = [
        ];

        $scope.transportMethods = ['Vessel','Barge','Rail','Pipeline','Tank Truck'];

        function onChangeSymbol() {

            registerFieldChange('symbol');

              }

        function getAutoDescription(symbolIsB) {
            return AUTO_TERMINAL_DESCRIPTION_PREFIX + (symbolIsB ? '- Physical Forward Agreement' : 'Futures Contract (CME: LPS)');
        }

        function isCurrentTerminalInfo(terminalInfo) {
            return !!(terminalInfo && terminalInfo.form === $scope.form);
        }

        function isMasterTerminalInfo(terminalInfo) {
            return !!(terminalInfo && terminalInfo.form === $scope.masterForm);
        }

        function isTerminalExists(terminalInfo) {
            return terminalInfo && terminalInfo.instance && terminalInfo.instance.id;
        }

        function isMasterFieldDisabled() {
            return $scope.isUpdate || $scope.form !== $scope.masterForm;
        }

        function hasTerminalInfoChanges(terminalInfo) {
            return !!(terminalInfo && terminalInfo.changes && terminalInfo.changes.length);
        }

        function getAutoClearingVenue(symbolIsB) {
            return symbolIsB ? 'Bilateral' : 'CME';
        }

        function getAutoTerminalName(symbolInfo) {
            var parts = [];

            if (symbolInfo.term === 'M') {
                parts.push.apply(parts, [
                    _.find(Teminals.months, {key: symbolInfo.symbolMonthYear.month}).shortName.toLocaleUpperCase(),
                    2000 + symbolInfo.symbolMonthYear.year
                ]);
            } else if (symbolInfo.term === 'Q') {
                parts.push.apply(parts, [
                    Terminals.getQuarterIndexByMonth(symbolInfo.symbolMonthYearFrom.month) + 1 + 'Q',
                    2000 + symbolInfo.symbolMonthYearFrom.year
                ]);
            }

            parts.push(symbolInfo.symbolIsB ? 'PFA' : 'SFC');

            return parts.join(' ');
        }

        function makeRequiredTerminalModels() {
            var rootTerminalInfo = $scope.relationsTreeRoot;
            var models = $scope.relatedModels = [];
            var isCurrent = function (terminalInfo) {
                return (terminalInfo.symbol === $scope.form.symbol) && (terminalInfo.term === $scope.form.term);
            };

        }

        function makeTerminalModel(terminalInfo, parentTerminalInfo) {
            terminalInfo.symbolInfo = terminalInfo.symbolInfo || Terminals.getTerminalSymbolInfo(terminalInfo);

            var parentInstance = parentTerminalInfo.form || parentTerminalInfo.instance;
            var modelForm = terminalInfo.form = {
                symbol: terminalInfo.symbol,
                term: terminalInfo.term,
                auctionDate: moment.utc(terminalInfo.auctionDate).startOf('day').valueOf(),
                showUser: false
            };

           return modelForm;
        }

        function onFieldChange(field) {
            registerFieldChange(field);

            var specificHandler = onFieldChange.specificHandlers[field.key];
            specificHandler && specificHandler();

        }

        onFieldChange.specificHandlers = {
            symbol: onChangeSymbol
        };

        function registerFieldChange(field) {
            if ($scope.terminalModel && $scope.terminalModel.id) {
                field = _.isString(field) ? self.formFieldsDict[field] : field;

                var originalValue = $scope.terminalModel[field.key];
                var newValue = Terminals.form.produceModelValueByFormField($scope.form, field);
                var isChanged = field.isModelValuesEqual ? !field.isModelValuesEqual(originalValue, newValue) : originalValue !== newValue;
                var changeInfo = {
                    key: field.key,
                    isChanged: isChanged,
                    field: field,
                    originalValue: originalValue,
                    newValue: newValue
                };
                var changes = $scope.terminalInfo.changes = $scope.terminalInfo.changes || {
                        length: 0
                    };

                if (isChanged) {
                    !changes[field.key] && changes.length++;
                    changes[field.key] = changeInfo;
                } else {
                    delete changes[field.key];
                    changes.length && changes.length--;
                }
            }
        }

        function registerFieldsChange(fields) {
            fields = fields || self.formFieldsDef;

            _.forEach(fields, registerFieldChange);
        }

        function saveTerminal(terminalForm) {
            var isUpdate = terminalForm.id;
            var model = Terminals.form.produceModelByForm(terminalForm, $scope.formDef, isUpdate);
            var submitUrl = isUpdate ? $scope.formDef.api.update : $scope.formDef.api.create;

            //Break down the transportModes into a single string
            console.log(model.transportModes);
            if(model.transportModes != null && typeof model.transportModes != 'string' && isUpdate){model.transportModes = model.transportModes.join(",")}

            return $q.when($http.post(submitUrl, model))
                .then(function (response) {
                    !isUpdate && response.data && (model.id = response.data);

                    return model;
                });
        }

        function saveAllTerminals() {
            var terminalForms = [];
            var root = $scope.relationsTreeRoot;
            var checkSaveNeeded = function (terminalInfo) {
                if (terminalInfo.form && (!terminalInfo.form.id || terminalInfo.changes && terminalInfo.changes.length)) {
                    terminalForms.unshift(terminalInfo.form);
                }
            };

            if (root) {
                checkSaveNeeded(root);
                _.forEach(root.related, function (relatedL1) {
                    checkSaveNeeded(relatedL1);
                    _.forEach(relatedL1.related, checkSaveNeeded);
                });
            } else {
                terminalForms.push($scope.masterForm);
            }

            var promises = _.map(terminalForms, saveTerminal);
            return $q.all(promises);
        }

        function setRemoteError(error) {
            if (arguments.length) {
                $scope.errorMessage = error ? error.message : 'Something going wrong';
                $scope.sendError = true;
            } else {
                $scope.errorMessage = '';
                $scope.sendError = false;
            }
        }

        this.formFieldsDef = [
            {
                label: 'Name',
                key: 'symbol',
                type: 'string',
                required: true,
                onChange: onFieldChange
            },
            {
                label: 'Location',
                key: 'location',
                type: 'string',
                required: true,
                onChange: onFieldChange
            },
            {
                label: 'State',
                key: 'state',
                type: 'select',
                required: true,
                options : $scope.stateCodes,
                onChange: onFieldChange
            },
            {
                label: 'Country',
                key: 'country',
                type: 'select',
                required: true,
                options : $scope.countryCodes,
                onChange: onFieldChange
            },
            {
                label: 'Continent',
                key: 'continent',
                type: 'select',
                required: true,
                options : $scope.continentCodes,
                onChange: onFieldChange
            },
            {
                label: 'Products',
                key: 'country',
                type: 'string',
                required: true,
                onChange: onFieldChange
            },
            {
                label: 'Independent',
                key: 'independent',
                type: 'select',
                required: true,
                options : $scope.YesNoCodes,
                onChange: onFieldChange
            },
            {
                label: 'Captive',
                key: 'captive',
                type: 'select',
                required: true,
                options : $scope.YesNoCodes,
                onChange: onFieldChange
            },
            {
                label: 'semi-captive',
                key: 'semicaptive',
                type: 'select',
                required: true,
                options : $scope.YesNoCodes,
                onChange: onFieldChange
            },
            {
                label: 'Existing',
                key: 'existing',
                type: 'select',
                required: true,
                options : $scope.YesNoCodes,
                onChange: onFieldChange
            },
            {
                label: 'Visible',
                key: 'showUser',
                type: 'boolean',
                required: false,
                onChange: onFieldChange
            },
            {
                label: 'Transport Modes',
                key: 'transportModes',
                type: 'multi',
                required: true,
                options : $scope.transportModeCodes,
                onChange: onFieldChange
            }

        ];
        this.formFieldsDict = _.keyBy(this.formFieldsDef, 'key');


        $scope.isUpdate = false;
        $scope.form = {};
        $scope.masterForm = null;
        $scope.terminalModel = originalModel;
        $scope.terminalInfo = {};
        $scope.opened = {};
        $scope.isSaving = false;
        $scope.formDef = {
            fields: this.formFieldsDef,
            api: {
                create: '/api/createTerminal',
                update: '/api/updateTerminal'
            }
        };
        $scope.relationsTreeRoot = null;
        $scope.isBusy = false;
        $scope.errorMessage = '';
        $scope.sendError = false;


        $scope.getFieldState = function (stateExp) {
            return Terminals.form.getFieldState($scope.isUpdate, stateExp);
        };

        $scope.getItems = _.memoize(function () {
            return Terminals.api.fetch();
        });

        $scope.send = function () {
            if (!$scope.isSaving && !$scope.isBusy) {

                $scope.isSaving = true;

                saveAllTerminals()  // TODO handle errors
                    .then(function (results) {
                        $scope.$close({
                            results: results
                        });
                    })
                    .catch(function (response) {
                        setRemoteError(response.data.error);
                    })
                    .finally(function () {
                        $scope.isSaving = false;
                    });
            }
        };

        $scope.cancel = function () {
            $uibModalInstance.dismiss('cancel');
        };

        $scope.getTreeNodeClassModifiers = function (terminalInfo) {
            var isExists = isTerminalExists(terminalInfo);
            var isCurrent = isCurrentTerminalInfo(terminalInfo);
            var isMaster = isMasterTerminalInfo(terminalInfo);
            var hasChanges = hasTerminalInfoChanges(terminalInfo);

            return terminalInfo ? [
                isExists ? '--exists' : (terminalInfo.form ? '--to-be-created' : '--not-exists'),
                isCurrent ? '--current' : '',
                isMaster ? '--master' : '',
                hasChanges ? '--modified' : ''
            ].join(' ') : '';
        };


        this.init();
    });

    module.controller('TerminalsFilterDialogCtrl', function ($q, $scope, currentFilter) {
        var self = this;

        this.fixDates = function () {
            if ($scope.model.today) {
                $scope.model.useAuctionDate = false;
            }
        };

        this.init = function () {
            var today = !!currentFilter.today;
            var auctionDate = currentFilter.auctionDate;
            var useAuctionDate = !today && !!auctionDate;
            var showUser = currentFilter.showUser == null ? -1 : !!currentFilter.showUser;

            auctionDate = auctionDate || moment.utc().startOf('day').valueOf();

            _.assign($scope.model, {
                today: today,
                auctionDate: auctionDate,
                useAuctionDate: useAuctionDate,
                showUser: showUser
            });
        };


        $scope.popups = {};

        $scope.model = {};
        
        $scope.options = {
            showUserVariants: [
                {name: 'Any', val: -1}, // we can't use null due to uib-btn-radio can't recognize it
                {name: 'Available', val: true},
                {name: 'Hidden', val: false}
            ]
        };


        $scope.openPopup = function (popupName) {
            _.forEach($scope.popups, function (value, name) {
                $scope.popups[name] = (popupName === name);
            });

            $scope.popups[popupName] = true;
        };

        $scope.cancel = function () {
            $scope.$dismiss('cancel');
        };

        $scope.send = function () {
            self.fixDates();

            $scope.$close({
                today: !!$scope.model.today,
                auctionDate: $scope.model.useAuctionDate && $scope.model.auctionDate || null,
                showUser: $scope.model.showUser === -1 ? null : $scope.model.showUser
            });
        };

        $scope.onChangeToday = function () {
            if ($scope.model.today) {
                $scope.model.useAuctionDate = false;
            }
        };

        $scope.onChangeUseAuctionDate = function () {
            if ($scope.model.useAuctionDate) {
                $scope.model.today = false;
            }
        };


        this.init();
    });

})(angular, angular.module('matrix-market'));