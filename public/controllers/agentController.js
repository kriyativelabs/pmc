pmcApp.controller('agentController', ['$scope', '$filter', '$location','$modal', '$log', 'apiService', 'cookieService', 'constantsService', 'DTOptionsBuilder', 'DTColumnDefBuilder',
    function ($scope, $filter, $location,$modal, $log, apiService, cookieService, constantsService, DTOptionsBuilder, DTColumnDefBuilder) {

        $scope.sNo = 1;
        $scope.getAgents = function () {
            apiService.GET("/users").then(function (response) {
                console.log(response);
                $scope.agents = response.data.data;
                $scope.agentsBackup = response.data.data;
            }, function (errorResponse) {
                if (errorResponse.status != 200) {
                    console.log(errorResponse);
                }
            });
        };
        
        var agentId = $location.search().id;
        if (!agentId) {
            $scope.isCreate = true;
        } else{
            $scope.code = $location.search().code;
            $scope.name = $location.search().name;
            $scope.isCreate = false;
        }

        $scope.delete = function (id, name) {
            var userConfirmation = confirm("Are you sure you want to delete agent:" + name);
            if (userConfirmation) {
                apiService.DELETE("/users/" + id).then(function (response) {
                    alert("Agent Successfully Deleted!");
                    $scope.getAgents();
                }, function (errorResponse) {
                    console.log(errorResponse);
                    alert(errorResponse.data.message);
                    if (errorResponse.status != 200) {
                        if (errorResponse.status == 304)
                            alert(errorResponse);
                    }
                });
            }
        };
        
        $scope.dtOptions = DTOptionsBuilder.newOptions()
            //.withColumnFilter()
            //.withDOM('<"input-group"f>pitrl')
            .withDOM('<"row"<"col-sm-6"i><"col-sm-6"p>>tr')
            .withPaginationType('full_numbers')
            .withDisplayLength(40)
            .withOption('language', {
                paginate: {
                    next: "",
                    previous: ""
                },
                search: "Search: ",
                lengthMenu: "_MENU_ records per page"
            });
        $scope.changeData = function (search) {
            $scope.agents = $filter('filter')($scope.agentsBackup, search);
        };
//############################################Modal###########################################
        $scope.open = function () {
            var modalInstance = $modal.open({
                templateUrl: 'agentModal.html',
                controller: AgentCreateCtrl
            });

            modalInstance.result.then(function (selected) {
                $scope.selected = selected;
            }, function () {
                $log.info('Modal dismissed at: ' + new Date());
            });
        };
//###########################################End##############################################

//############################################Modal###########################################
        $scope.openUpdate = function (agentId, agentName, contactNo, email, loginId, accountType) {
            var modalInstance = $modal.open({
                templateUrl: 'agentModal.html',
                controller: AgentUpdateCtrl,
                resolve: {
                    agentId: function () {
                        return agentId;
                    },
                    agentName: function () {
                        return agentName;
                    },
                    agentContactNo: function () {
                        return contactNo;
                    },
                    agentEmail: function () {
                        return email;
                    },
                    agentLoginId: function () {
                        return loginId;
                    },
                    agentAccountType: function () {
                        return accountType;
                    }
                }
            });

            modalInstance.result.then(function (selected) {
                $scope.selected = selected;
            }, function () {
                $log.info('Modal dismissed at: ' + new Date());
            });
        };
//################End##########

     }]);

var AgentCreateCtrl = function ($scope, $modalInstance, $location, apiService) {
    $scope.title = "Create";

    $scope.ok = function () {
        $modalInstance.close($scope.dt);
    };
    $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
    };

    $scope.createOrUpdate = function () {
        var createObj = {};
        createObj.name = $scope.name;
        createObj.contactNo = parseInt($scope.contactNo);
        createObj.password = "";
        createObj.address = "";
        createObj.email = $scope.email;
        createObj.loginId = $scope.loginId;
        createObj.accountType = "AGENT";
        createObj.companyId = -1;

        apiService.POST("/users", createObj).then(function (response) {
            console.log(response.data.data);
            $scope.alerts = [];
            $scope.alerts.push({type: 'success', msg: "Agent Successfully Created!"});
            $location.path("/agents");
        }, function (errorResponse) {
            $scope.alerts = [];
            $scope.alerts.push({ type: 'danger', msg: errorResponse.data.message});
            if (errorResponse.status != 200) {
                console.log(errorResponse);
            }
            $scope.code = "";
        });
    };

    $scope.closeAlert = function (index) {
        $scope.alerts.splice(index, 1);
    };
};


var AgentUpdateCtrl = function ($scope, $modalInstance, $location, apiService, agentId,agentName,agentContactNo, agentEmail,agentLoginId, agentAccountType) {
    $scope.title = "Update";
    $scope.isUpdate = true;

    $scope.ok = function () {
        $modalInstance.close($scope.dt);
    };

    $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
    };

    $scope.name = agentName;
    $scope.contactNo = agentContactNo;
    $scope.email = agentEmail;
    $scope.loginId = agentLoginId;
    $scope.accountType = agentAccountType;

    $scope.createOrUpdate = function () {
        var createObj = {};
        createObj.id = parseInt(agentId);
        createObj.name = $scope.name;
        createObj.contactNo = parseInt($scope.contactNo);
        createObj.password = "";
        createObj.address = "";
        createObj.email = $scope.email;
        createObj.loginId = $scope.loginId;
        createObj.accountType = $scope.accountType;
        createObj.companyId = -1;

        apiService.PUT("/users/" + agentId, createObj).then(function (response) {
            console.log(response.data.data);
            alert("Agent Successfully Updated!");
            $location.path("/agents");
        }, function (errorResponse) {
            alert(errorResponse.data.message);
            if (errorResponse.status != 200) {
                console.log(errorResponse);
            }
        });
    };

    $scope.closeAlert = function (index) {
        $scope.alerts.splice(index, 1);
    };
};
