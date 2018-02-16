package com.github.utransnet.simulator.externalapi.h2impl;

import com.github.utransnet.simulator.externalapi.*;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Artem on 13.02.2018.
 */
public class ExternalAPIH2 extends ExternalAPI {


    private final TransferOperationH2Repository transferOperationRepository;
    private final APIObjectFactoryH2 apiObjectFactory;
    private final MessageOperationRepository messageOperationRepository;
    private final ProposalH2Repository proposalRepository;

    ExternalAPIH2(
            TransferOperationH2Repository transferOperationRepository,
            APIObjectFactoryH2 apiObjectFactory,
            MessageOperationRepository messageOperationRepository,
            ProposalH2Repository proposalRepository
    ) {
        this.transferOperationRepository = transferOperationRepository;
        this.apiObjectFactory = apiObjectFactory;
        this.messageOperationRepository = messageOperationRepository;
        this.proposalRepository = proposalRepository;
    }

    @Override
    public void sendProposal(
            UserAccount from,
            UserAccount to,
            UserAccount proposingAccount,
            UserAccount feePayer,
            Asset asset, long amount) {
        TransferOperationH2 transferOperation = new TransferOperationH2(
                apiObjectFactory, from, to, asset, amount, ""
        );
        ProposalH2 proposalH2 = new ProposalH2(
                apiObjectFactory, proposingAccount, feePayer, transferOperation
        );
        proposalRepository.save(proposalH2);
    }

    @Override
    public void approveProposal(UserAccount approvingAccount, Proposal proposal) {
        ProposalH2 proposalH2 = (ProposalH2) proposal;
        if(proposalH2.neededApproves().contains(approvingAccount.getId())) {
            proposalH2.addApprove(approvingAccount);
            proposalRepository.save(proposalH2);
            if(proposalH2.approved()){
                BaseOperation operation = proposalH2.getOperation();
                switch (operation.getOperationType()){
                    case TRANSFER:
                        if(operation instanceof TransferOperationH2) {
                            transferOperationRepository.save((TransferOperationH2)operation);
                        }
                        break;
                    case MESSAGE:
                        if(operation instanceof MessageOperationH2) {
                            messageOperationRepository.save((MessageOperationH2)operation);
                        }
                        break;
                }
                proposalRepository.delete(proposalH2);
            }
        }
    }

    @Override
    public void sendAsset(UserAccount from, UserAccount to, AssetAmount assetAmount, String memo) {
        TransferOperationH2 transferOperationH2 = new TransferOperationH2(
                apiObjectFactory, from, to, assetAmount, memo
        );
        transferOperationH2.setCreationDate(Instant.now());
        transferOperationRepository.save(transferOperationH2);
    }

    @Override
    public void sendMessage(UserAccount from, UserAccount to, String message) {
        MessageOperationH2 messageOperationH2 = new MessageOperationH2(apiObjectFactory, from, to, message);
        messageOperationH2.setCreationDate(Instant.now());
        messageOperationRepository.save(messageOperationH2);
    }

    @Override
    public List<? extends BaseOperation> getAccountHistory(UserAccount account, OperationType operationType) {
        switch (operationType) {
            case TRANSFER:
                return transferOperationRepository.findByToOrFrom(account.getId(), account.getId());
            case MESSAGE:
                return messageOperationRepository.findByToOrFrom(account.getId(), account.getId());
        }
        return new ArrayList<>(0);
    }

    @Override
    public List<? extends BaseOperation> getAccountHistory(UserAccount account) {
        ArrayList<BaseOperationH2> operations = new ArrayList<>();
        operations.addAll(transferOperationRepository.findByToOrFrom(account.getId(), account.getId()));
        operations.addAll(messageOperationRepository.findByToOrFrom(account.getId(), account.getId()));
        return operations.stream().sorted(Comparator.comparing(o -> o.creationDate)).collect(Collectors.toList());
    }

    @Override
    public UserAccount createAccount(String name) {
        return apiObjectFactory.userAccount(name);
    }

    @Override
    public UserAccount getAccountByName(String name) {
        return createAccount(name);
    }

    @Override
    public Optional<? extends BaseOperation> getLastOperation(UserAccount account) {
        List<? extends BaseOperation> operations = getAccountHistory(account);
        if(operations.isEmpty()){
            return Optional.empty();
        } else {
            return Optional.of(operations.get(operations.size() - 1));
        }
    }

    @Override
    public List<? extends BaseOperation> operationsAfter(UserAccount account, String operationId) {
        List<? extends BaseOperation> history = getAccountHistory(account);
        Optional<? extends BaseOperation> operation = history
                .stream()
                .filter(o -> Objects.equals(o.getId(), operationId))
                .findFirst();
        return operation
                .<List<? extends BaseOperation>>map(
                        baseOperation -> history.subList(history.indexOf(baseOperation), history.size() - 1)
                )
                .orElseGet(() -> new ArrayList<>(0));
    }

}
