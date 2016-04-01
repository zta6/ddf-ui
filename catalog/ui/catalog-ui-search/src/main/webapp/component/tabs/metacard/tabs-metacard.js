/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define*/
define([
    'underscore',
    '../tabs',
    'js/store',
    'component/metacard-basic/metacard-basic.view',
    'component/metacard-advanced/metacard-advanced.view'
], function (_, Tabs, store, MetacardBasicView, MetacardAdvancedView) {

    var MetacardTabs = Tabs.extend({
        defaults: {
            tabs: {
                'Basic': MetacardBasicView,
                'Advanced': MetacardAdvancedView,
                'History': MetacardBasicView,
                'Associations': MetacardBasicView,
                'Sharing': MetacardBasicView,
                'Actions': MetacardBasicView
            }
        }
    });

    return MetacardTabs;
});