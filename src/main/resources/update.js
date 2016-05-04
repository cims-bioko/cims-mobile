var imports = JavaImporter(
    org.openhds.mobile.R,
    org.openhds.mobile.navconfig,
    org.openhds.mobile.navconfig.forms,
    org.openhds.mobile.navconfig.forms.filters,
    org.openhds.mobile.navconfig.forms.builders,
    org.openhds.mobile.navconfig.forms.consumers,
    org.openhds.mobile.fragment.navigate.detail,
    org.openhds.mobile.repository.search.SearchUtils
);

with (imports) {

    var labels = {};
    var binds = {};

    function bind(b) {
        var bind_name = b.name || b.form;
        binds[bind_name] = new Binding({
            getName: function() { return bind_name; },
            getForm: function() { return b.form; },
            getLabel: function() { return config.getString(b.label); },
            getBuilder: function() { return b.builder; },
            getConsumer: function() { return b.consumer || new DefaultConsumer(); },
            getSearches: function() { return b.searches || []; },
            requiresSearch: function() { return b.searches? b.searches.length > 0 : false; }
        });
        labels[b.form] = b.label;  // temporary: form labels will come directly from bindings
    }

    bind({ form: 'visit',
           label: 'visitFormLabel',
           builder: new UpdateFormPayloadBuilders.StartAVisit(),
           consumer: new UpdateFormPayloadConsumers.StartAVisit() });

    var migrantSearch = SearchUtils.getIndividualModule(
        ProjectFormFields.Individuals.INDIVIDUAL_UUID, R.string.search_individual_label);

    bind({ name: 'internal_in_migration',
           form: 'in_migration',
           label: 'inMigrationFormLabel',
           builder: new UpdateFormPayloadBuilders.RegisterInternalInMigration(),
           consumer: new UpdateFormPayloadConsumers.RegisterInMigration(),
           searches: [ migrantSearch ] });

    bind({ name: 'external_in_migration',
           form: 'in_migration',
           label: 'inMigrationFormLabel',
           builder: new UpdateFormPayloadBuilders.RegisterExternalInMigration(),
           consumer: new UpdateFormPayloadConsumers.RegisterInMigration() });

    bind({ name: 'in-migrant',
           form: 'individual',
           label: 'individualFormLabel',
           builder: new UpdateFormPayloadBuilders.AddIndividualFromInMigration(),
           consumer: new UpdateFormPayloadConsumers.AddIndividualFromInMigration(binds['external_in_migration']) });

    bind({ form: 'out_migration',
           label: 'outMigrationFormLabel',
           builder: new UpdateFormPayloadBuilders.RegisterOutMigration(),
           consumer: new UpdateFormPayloadConsumers.RegisterOutMigration() });

    bind({ form: 'death',
           label: 'deathFormLabel',
           builder: new UpdateFormPayloadBuilders.RegisterDeath(),
           consumer: new UpdateFormPayloadConsumers.RegisterDeath() });

    bind({ name: 'update_preg_obs',
           form: 'pregnancy_observation',
           label: 'pregnancyObservationFormLabel',
           builder: new UpdateFormPayloadBuilders.RecordPregnancyObservation() });

    var paternitySearch = SearchUtils.getIndividualModule(
        ProjectFormFields.PregnancyOutcome.FATHER_UUID, R.string.search_father_label);

    bind({ form: 'pregnancy_outcome',
           label: 'pregnancyOutcomeFormLabel',
           builder: new UpdateFormPayloadBuilders.RecordPregnancyOutcome(),
           searches: [ paternitySearch ] });

    function launcher(l) {
        return new Launcher({
            getLabel: function() { return config.getString(l.label); },
            relevantFor: function(ctx) { return l.filter? l.filter.shouldDisplay(ctx) : true; },
            getBinding: function() { return binds[l.bind]; }
        });
    }

    var launchers = {
        individual: [
            launcher({ label: 'shared.visitLabel',
                       bind: 'visit',
                       filter: new UpdateFormFilters.StartAVisit()}),
            launcher({ label: 'update.internalInMigrationLabel',
                       bind: 'internal_in_migration',
                       filter: new UpdateFormFilters.RegisterInMigration() }),
            launcher({ label: 'update.externalInMigrationLabel',
                       bind: 'in-migrant',
                       filter: new UpdateFormFilters.RegisterInMigration() })
        ],
        bottom: [
            launcher({ label: 'update.outMigrationLabel',
                       bind: 'out_migration',
                       filter: new UpdateFormFilters.DeathOrOutMigrationFilter() }),
            launcher({ label: 'update.deathLabel',
                       bind: 'death',
                       filter: new UpdateFormFilters.DeathOrOutMigrationFilter() }),
            launcher({ label: 'shared.pregnancyObservationLabel',
                       bind: 'update_preg_obs',
                       filter: new UpdateFormFilters.PregnancyFilter() }),
            launcher({ label: 'update.pregnancyOutcomeLabel',
                       bind: 'pregnancy_outcome',
                       filter: new UpdateFormFilters.PregnancyFilter() })
        ]
    };

    var details = {
        bottom: new IndividualDetailFragment()
    };

    var module = new NavigatorModule({
        getActivityTitle: function() { return config.getString('update.activityTitle'); },
        getLaunchLabel: function() { return config.getString('update.launchTitle'); },
        getLaunchDescription: function() { return config.getString('update.launchDescription'); },
        getLaunchers: function(level) { return launchers[level] || []; },
        getDetailFragment: function(level) { return details[level] || null; },
        getFormLabels: function() { return labels; }
    });

    config.addModule(module);
}