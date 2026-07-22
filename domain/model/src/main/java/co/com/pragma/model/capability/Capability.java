package co.com.pragma.model.capability;

import co.com.pragma.model.capability.valueobject.CapabilityDescription;
import co.com.pragma.model.capability.valueobject.CapabilityName;
import co.com.pragma.model.exceptions.FieldsValidationException;

import java.util.LinkedHashMap;
import java.util.Map;

public class Capability {

    private final Long id;
    private final CapabilityName name;
    private final CapabilityDescription description;
    private final CapabilityStatusEnum status;
    private final Integer technologyCount;

    private Capability(Builder builder) {
        this.id = builder.id;
        this.name = new CapabilityName(builder.name);
        this.description = new CapabilityDescription(builder.description);
        this.status = builder.status;
        this.technologyCount = builder.technologyCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public CapabilityName getName() {
        return name;
    }

    public CapabilityDescription getDescription() {
        return description;
    }

    public CapabilityStatusEnum getStatus() {
        return status;
    }

    public Integer getTechnologyCount() {
        return technologyCount;
    }

    public static class Builder {

        private Long id;
        private String name;
        private String description;
        private CapabilityStatusEnum status;
        private Integer technologyCount;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder status(CapabilityStatusEnum status) {
            this.status = status;
            return this;
        }

        public Builder technologyCount(Integer technologyCount) {
            this.technologyCount = technologyCount;
            return this;
        }

        public Capability build() {
            Map<String, String> errors = new LinkedHashMap<>();

            CapabilityName.validate(name, errors);
            CapabilityDescription.validate(description, errors);

            if (!errors.isEmpty())
                throw new FieldsValidationException(errors);

            return new Capability(this);
        }
    }
}
