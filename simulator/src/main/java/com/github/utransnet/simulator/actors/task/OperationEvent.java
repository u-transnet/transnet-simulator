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
public class OperationEvent <T> {

    Type eventType;

    T object;

    public enum Type {
        TRANSFER, MESSAGE, PROPOSAL_CREATE, PROPOSAL_UPDATE, PROPOSAL_DELETE
    }

    public static class ProposalCreateEvent extends OperationEvent<Proposal> {
        public ProposalCreateEvent(Proposal object) {
            super(PROPOSAL_CREATE, object);
        }
    }

    public static class ProposalUpdateEvent extends OperationEvent<Proposal> {
        public ProposalUpdateEvent(Proposal object) {
            super(PROPOSAL_UPDATE, object);
        }
    }

    public static class ProposalDeleteEvent extends OperationEvent<String> {
        public ProposalDeleteEvent(String proposalId) {
            super(PROPOSAL_DELETE, proposalId);
        }
    }

    public static class TransferEvent extends OperationEvent<TransferOperation> {
        public TransferEvent(TransferOperation transferOperation) {
            super(TRANSFER, transferOperation);
        }
    }

    public static class MessageEvent extends OperationEvent<MessageOperation> {
        public MessageEvent(MessageOperation messageOperation) {
            super(MESSAGE, messageOperation);
        }
    }
}
