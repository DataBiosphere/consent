package org.broadinstitute.consent.http.job;


import de.spinscale.dropwizard.jobs.Job;
import de.spinscale.dropwizard.jobs.annotations.On;
import de.spinscale.dropwizard.jobs.annotations.OnApplicationStart;
import org.broadinstitute.consent.http.service.validate.AbstractUseRestrictionValidatorAPI;
import org.broadinstitute.consent.http.service.validate.UseRestrictionValidatorAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@OnApplicationStart
@On("0 00 00 * * ?")
public class UseRestrictionValidationJob  extends Job {

    private static final Logger logger = LoggerFactory.getLogger(UseRestrictionValidationJob.class);
    private UseRestrictionValidatorAPI useRestrictionValidator;


    @Override
    public void doJob() {
        logger.info("validating consent use restrictions");
        getUseRestrictionValidator().validateConsentUseRestriction();
        logger.info("validating dar use restrictions");
        getUseRestrictionValidator().validateDARUseRestriction();
    }

    protected void setUseRestrictionValidator(UseRestrictionValidatorAPI useRestrictionValidator){
        this.useRestrictionValidator = useRestrictionValidator;
    }

    protected  UseRestrictionValidatorAPI getUseRestrictionValidator(){
        if(this.useRestrictionValidator == null){
            this.useRestrictionValidator = AbstractUseRestrictionValidatorAPI.getInstance();
        }
        return this.useRestrictionValidator;
    }

}
