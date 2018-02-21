package com.github.utransnet.simulator.actors.task;


import com.github.utransnet.simulator.externalapi.Proposal;
import com.github.utransnet.simulator.externalapi.operations.MessageOperation;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.github.utransnet.simulator.actors.task.OperationEvent.Type.*;

/**
 * Created by Artem on 19.02.2018.
 */
@Getter
@AllArgsConstructor
public class OperationEvent {

    Type eventType;

    Object object;

    public enum Type {
        TRANSFER, MESSAGE, PROPOSAL_CREATE, PROPOSAL_UPDATE, PROPOSAL_DELETE
    }

    public static class ProposalCreateEvent extends OperationEvent {
        public ProposalCreateEvent(Proposal object) {
            super(PROPOSAL_CREATE, object);
        }

        @Override
        public Proposal getObject() {
            return (Proposal) super.getObject();
        }
    }

    public static class ProposalUpdateEvent extends OperationEvent {
        public ProposalUpdateEvent(Proposal object) {
            super(PROPOSAL_UPDATE, object);
        }

        @Override
        public Proposal getObject() {
            return (Proposal) super.getObject();
        }
    }

    public static class ProposalDeleteEvent extends OperationEvent {
        public ProposalDeleteEvent(String proposalId) {
            super(PROPOSAL_DELETE, proposalId);
        }

        @Override
        public String getObject() {
            return (String) super.getObject();
        }
    }

    public static class TransferEvent extends OperationEvent {
        public TransferEvent(TransferOperation transferOperation) {
            super(TRANSFER, transferOperation);
        }


        @Override
        public TransferOperation getObject() {
            return (TransferOperation) super.getObject();
        }
    }

    public static class MessageEvent extends OperationEvent {
        public MessageEvent(MessageOperation messageOperation) {
            super(MESSAGE, messageOperation);
        }

        @Override
        public MessageOperation getObject() {
            return (MessageOperation) super.getObject();
        }
    }
}
