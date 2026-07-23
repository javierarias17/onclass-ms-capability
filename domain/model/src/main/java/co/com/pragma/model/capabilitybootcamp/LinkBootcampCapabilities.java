package co.com.pragma.model.capabilitybootcamp;

import co.com.pragma.model.capabilitybootcamp.valueobject.BootcampCapabilityIds;
import co.com.pragma.model.capabilitybootcamp.valueobject.BootcampId;
import co.com.pragma.model.exceptions.FieldsValidationException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LinkBootcampCapabilities {

    private final BootcampId bootcampId;
    private final BootcampCapabilityIds capabilityIds;

    private LinkBootcampCapabilities(Builder builder) {
        this.bootcampId = new BootcampId(builder.bootcampId);
        this.capabilityIds = new BootcampCapabilityIds(builder.capabilityIds);
    }

    public static Builder builder() {
        return new Builder();
    }

    public BootcampId getBootcampId() {
        return bootcampId;
    }

    public BootcampCapabilityIds getCapabilityIds() {
        return capabilityIds;
    }

    public static class Builder {

        private Long bootcampId;
        private List<Long> capabilityIds;

        public Builder bootcampId(Long bootcampId) {
            this.bootcampId = bootcampId;
            return this;
        }

        public Builder capabilityIds(List<Long> capabilityIds) {
            this.capabilityIds = capabilityIds;
            return this;
        }

        public LinkBootcampCapabilities build() {
            Map<String, String> errors = new LinkedHashMap<>();

            BootcampId.validate(bootcampId, errors);
            BootcampCapabilityIds.validate(capabilityIds, errors);

            if (!errors.isEmpty())
                throw new FieldsValidationException(errors);

            return new LinkBootcampCapabilities(this);
        }
    }
}
