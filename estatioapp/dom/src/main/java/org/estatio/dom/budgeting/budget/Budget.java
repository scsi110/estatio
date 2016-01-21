/*
 *
 *  Copyright 2012-2015 Eurocommercial Properties NV
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.estatio.dom.budgeting.budget;

import org.apache.isis.applib.annotation.*;
import org.apache.isis.applib.services.i18n.TranslatableString;
import org.estatio.dom.EstatioDomainObject;
import org.estatio.dom.WithIntervalMutable;
import org.estatio.dom.apptenancy.WithApplicationTenancyProperty;
import org.estatio.dom.asset.Property;
import org.estatio.dom.budgeting.budgetitem.BudgetItem;
import org.estatio.dom.budgeting.keyitem.contributions.OccupanciesOnKeyItemContributions;
import org.estatio.dom.budgeting.viewmodels.BudgetOverview;
import org.estatio.dom.lease.LeaseItems;
import org.estatio.dom.lease.LeaseTerms;
import org.estatio.dom.valuetypes.LocalDateInterval;
import org.isisaddons.module.security.dom.tenancy.ApplicationTenancy;
import org.joda.time.LocalDate;

import javax.inject.Inject;
import javax.jdo.annotations.*;
import java.math.BigDecimal;
import java.util.SortedSet;
import java.util.TreeSet;

@javax.jdo.annotations.PersistenceCapable(
        identityType = IdentityType.DATASTORE
//      ,schema = "budget"
)
@javax.jdo.annotations.DatastoreIdentity(
        strategy = IdGeneratorStrategy.NATIVE,
        column = "id")
@javax.jdo.annotations.Version(
        strategy = VersionStrategy.VERSION_NUMBER,
        column = "version")
@javax.jdo.annotations.Queries({
        @Query(
                name = "findByProperty", language = "JDOQL",
                value = "SELECT " +
                        "FROM org.estatio.dom.budgeting.budget.Budget " +
                        "WHERE property == :property "),
        @Query(
                name = "findByPropertyAndStartDate", language = "JDOQL",
                value = "SELECT " +
                        "FROM org.estatio.dom.budgeting.budget.Budget " +
                        "WHERE property == :property && startDate == :startDate")
})
@DomainObject(editing = Editing.DISABLED, autoCompleteRepository = BudgetRepository.class)
public class Budget extends EstatioDomainObject<Budget> implements WithIntervalMutable<Budget>, WithApplicationTenancyProperty {

    public Budget() {
        super("property, startDate, endDate");
    }

    public Budget(final LocalDate startDate, final LocalDate endDate) {
        this();
        this.startDate = startDate;
        this.endDate = endDate;
    }

    //region > identificatiom
    public TranslatableString title() {
        return TranslatableString.tr("{name}", "name", "Budget for ".concat(getProperty().getName())
                .concat(" - period: ")
                .concat(getEffectiveInterval().toString())
        );
    }
    //endregion

    private Property property;

    @javax.jdo.annotations.Column(name="propertyId", allowsNull = "false")
    @MemberOrder(sequence = "1")
    @PropertyLayout(hidden = Where.PARENTED_TABLES)
    public Property getProperty() {
        return property;
    }

    public void setProperty(Property property) {
        this.property = property;
    }

    // //////////////////////////////////////

    private LocalDate startDate;

    @MemberOrder(sequence = "2")
    @javax.jdo.annotations.Column(allowsNull = "true")
    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    // //////////////////////////////////////

    private LocalDate endDate;

    @MemberOrder(sequence = "3")
    @javax.jdo.annotations.Column(allowsNull = "true")
    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    // //////////////////////////////////////

    @Programmatic
    public LocalDateInterval getInterval() {
        return LocalDateInterval.including(getStartDate(), getEndDate());
    }

    @Programmatic
    public LocalDateInterval getEffectiveInterval() {
        return getInterval();
    }

    // //////////////////////////////////////

    public boolean isCurrent() {
        return isActiveOn(getClockService().now());
    }

    private boolean isActiveOn(final LocalDate date) {
        return LocalDateInterval.including(this.getStartDate(), this.getEndDate()).contains(date);
    }

    // //////////////////////////////////////

    private WithIntervalMutable.Helper<Budget> changeDates = new WithIntervalMutable.Helper<Budget>(this);

    WithIntervalMutable.Helper<Budget> getChangeDates() {
        return changeDates;
    }

    @Override
    @Action(semantics = SemanticsOf.IDEMPOTENT, hidden = Where.EVERYWHERE)
    public Budget changeDates(
            final @ParameterLayout(named = "Start date") @Parameter(optionality = Optionality.OPTIONAL) LocalDate startDate,
            final @ParameterLayout(named = "End date") @Parameter(optionality =  Optionality.OPTIONAL) LocalDate endDate) {
        return getChangeDates().changeDates(startDate, endDate);
    }

    @Override
    public LocalDate default0ChangeDates() {
        return getChangeDates().default0ChangeDates();
    }

    @Override
    public LocalDate default1ChangeDates() {
        return getChangeDates().default1ChangeDates();
    }

    @Override
    public String validateChangeDates(
            final LocalDate startDate,
            final LocalDate endDate) {

        if (budgetRepository.validateNewBudget(getProperty(),startDate,endDate) != null) {
            for (Budget budget : budgetRepository.findByProperty(property)) {
                if (!budget.equals(this) && budget.getInterval().overlaps(new LocalDateInterval(startDate, endDate))) {
                    return "A budget cannot overlap an existing budget.";
                }
            }
        }
        return getChangeDates().validateChangeDates(startDate, endDate);
    }

    // //////////////////////////////////////

    private SortedSet<BudgetItem> items = new TreeSet<BudgetItem>();

    @CollectionLayout(render= RenderType.EAGERLY)
    @Persistent(mappedBy = "budget", dependentElement = "true")
    public SortedSet<BudgetItem> getItems() {
        return items;
    }

    public void setItems(final SortedSet<BudgetItem> items) {
        this.items = items;
    }

    // //////////////////////////////////////

    @MemberOrder(sequence = "4")
    @PropertyLayout(hidden = Where.EVERYWHERE)
    @Override public ApplicationTenancy getApplicationTenancy() {
        return getProperty().getApplicationTenancy();
    }

    // //////////////////////////////////////

    @Action(restrictTo = RestrictTo.PROTOTYPING)
    @ActionLayout()
    public Budget removeAllBudgetItems(@ParameterLayout(named = "Are you sure?") final boolean confirmDelete) {
        for (BudgetItem budgetItem : this.getItems()) {

            getContainer().remove(budgetItem);
            getContainer().flush();

        }

        return this;
    }

    public String validateRemoveAllBudgetItems(boolean confirmDelete){
        return confirmDelete? null:"Please confirm";
    }

    // //////////////////////////////////////

    @Programmatic
    public BigDecimal getTotalBudgetedValue(){
        BigDecimal total = BigDecimal.ZERO;
        for (BudgetItem item : getItems()){
            total = total.add(item.getBudgetedValue());
        }
        return total;
    }

    public BudgetOverview BudgetOverview(){
        return new BudgetOverview(this);
    }

    @Inject
    private LeaseItems leaseItems;

    @Inject
    private OccupanciesOnKeyItemContributions occupancyContributionsForBudgets;

    @Inject
    private LeaseTerms leaseTerms;

    @Inject
    private BudgetRepository budgetRepository;

}
