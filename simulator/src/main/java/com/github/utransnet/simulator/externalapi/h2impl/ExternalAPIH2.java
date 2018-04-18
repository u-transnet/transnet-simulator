package com.github.utransnet.simulator.externalapi.h2impl;

import com.github.utransnet.simulator.externalapi.*;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import lombok.SneakyThrows;

import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by Artem on 13.02.2018.
 */
public class ExternalAPIH2 extends ExternalAPI {


    private final TransferOperationH2Repository transferOperationRepository;
    private final APIObjectFactoryH2 apiObjectFactory;
    private final MessageOperationRepository messageOperationRepository;
    private final ProposalH2Repository proposalRepository;

    private final Map<String, AccountUpdateListener<AccountUpdateObject>> accountUpdateListeners = new HashMap<>(32);
    private final Map<String, AccountUpdateListener<ExternalObject>> accountOperationListeners = new HashMap<>(32);

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
            UserAccount feePayer,
            AssetAmount assetAmount,
            String memo
    ) {
        TransferOperationH2 transferOperation = new TransferOperationH2(
                apiObjectFactory, from, to, assetAmount, memo
        );
        ProposalH2 proposalH2 = new ProposalH2(
                apiObjectFactory, from, feePayer, transferOperation
        );
        proposalH2.setCreationDate(Instant.now());
        proposalRepository.save(proposalH2);

        Set<String> accsToNotify = Stream.of(
                from.getId(),
                to.getId(),
                feePayer.getId()
        ).collect(Collectors.toSet());
        fireAccountOperation(accsToNotify, proposalH2);
        fireAccountUpdate(accsToNotify);
    }

    @Override
    @SneakyThrows
    public void approveProposal(UserAccount approvingAccount, Proposal proposal) {
        ProposalH2 proposalH2 = (ProposalH2) proposal;

        Set<String> accsToNotify = new HashSet<>(10);
        accsToNotify.add(approvingAccount.getId());
        ExternalObject updaterObject = proposalH2;

        if (proposalH2.neededApprovals().contains(approvingAccount.getId())) {
            proposalH2.addApprove(approvingAccount);
            proposalRepository.save(proposalH2);
            BaseOperation operation = proposalH2.getOperation();
            switch (operation.getOperationType()) {
                case TRANSFER:
                    if (operation instanceof TransferOperationH2) {
                        TransferOperationH2 proposedOperation = (TransferOperationH2) operation;
                        accsToNotify.add(proposedOperation.getFromStr());
                        accsToNotify.add(proposedOperation.getToStr());
                        if (proposalH2.approved()) {
                            proposedOperation.setCreationDate(Instant.now());
                            transferOperationRepository.save(proposedOperation);

                            // don't know why, but if we clear proposalH2 object directly,
                            // approvesAdded won't be cleared
                            /*ProposalH2 tmp = proposalRepository.findOne(Long.parseLong(proposalH2.getId()));
                            tmp.clear();
                            proposalRepository.save(tmp);
                            proposalRepository.delete(tmp);*/
                            updaterObject = proposedOperation;
                        }
                    }
                    break;
                case MESSAGE:
                    if (operation instanceof MessageOperationH2) {
                        MessageOperationH2 operation1 = (MessageOperationH2) operation;
                        accsToNotify.add(operation1.getFromStr());
                        accsToNotify.add(operation1.getToStr());
                        if (proposalH2.approved()) {
                            messageOperationRepository.save(operation1);
//                            proposalRepository.delete(proposalH2);
                            updaterObject = operation1;
                        }
                    }
                    break;
            }

            fireAccountOperation(accsToNotify, new ProposalUpdateOperationH2(proposal, accsToNotify));
            fireAccountOperation(accsToNotify, updaterObject);
            fireAccountUpdate(accsToNotify);
        }
    }

    @Override
    public void sendAsset(UserAccount from, UserAccount to, AssetAmount assetAmount, String memo) {
        TransferOperationH2 transferOperationH2 = new TransferOperationH2(
                apiObjectFactory, from, to, assetAmount, memo
        );
        transferOperationH2.setCreationDate(Instant.now());
        transferOperationRepository.save(transferOperationH2);

        Set<String> accsToNotify = Stream.of(from.getId(), to.getId()).collect(Collectors.toSet());
        fireAccountOperation(accsToNotify, transferOperationH2);
        fireAccountUpdate(accsToNotify);
    }

    @Override
    public void sendMessage(UserAccount from, UserAccount to, String message) {
        MessageOperationH2 messageOperationH2 = new MessageOperationH2(apiObjectFactory, from, to, message);
        messageOperationH2.setCreationDate(Instant.now());
        messageOperationRepository.save(messageOperationH2);

        Set<String> accsToNotify = Stream.of(from.getId(), to.getId()).collect(Collectors.toSet());
        fireAccountOperation(accsToNotify, messageOperationH2);
        fireAccountUpdate(accsToNotify);
    }

    @Override
    public List<? extends BaseOperationH2> getAccountHistory(UserAccount account, OperationType operationType) {
        switch (operationType) {
            case TRANSFER:
                return transferOperationRepository.findByToOrFrom(account.getId(), account.getId())
                        .stream()
                        .peek(op -> op.setApiObjectFactory(apiObjectFactory))
                        .sorted(Comparator.comparing(o -> o.creationDate))
                        .collect(Collectors.toList());
            case MESSAGE:
                return messageOperationRepository.findByToOrFrom(account.getId(), account.getId())
                        .stream()
                        .peek(op -> op.setApiObjectFactory(apiObjectFactory))
                        .sorted(Comparator.comparing(o -> o.creationDate))
                        .collect(Collectors.toList());
            case PROPOSAL_CREATE:
                ArrayList<BaseOperationH2> operations = new ArrayList<>();
                Iterable<ProposalH2> allProposals = proposalRepository.findAll();
                allProposals.forEach(proposalH2 -> {
                    proposalH2.setApiObjectFactory(apiObjectFactory);
                    BaseOperation operation = proposalH2.getOperation();
                    if (proposalH2.neededApprovals().contains(account.getId())
                            || operation.getAffectedAccounts().contains(account.getId())) {
                        HashSet<String> set = new HashSet<>(proposalH2.neededApprovals());
                        set.addAll(operation.getAffectedAccounts());
                        ProposalCreateOperationH2 proposalCreateOperation = new ProposalCreateOperationH2(proposalH2, set);
                        proposalCreateOperation.creationDate = proposalH2.creationDate;
                        proposalCreateOperation.id = Long.parseLong(proposalH2.getId());
                        operations.add(proposalCreateOperation);
                    }
                });
                return operations.stream()
                        .peek(op -> op.setApiObjectFactory(apiObjectFactory))
                        .sorted(Comparator.comparing(o -> o.creationDate))
                        .collect(Collectors.toList());
        }
        return new ArrayList<>(0);
    }

    @Override
    public List<? extends BaseOperation> getAccountHistory(UserAccount account) {
        ArrayList<BaseOperationH2> operations = new ArrayList<>();
        operations.addAll(getAccountHistory(account, OperationType.TRANSFER));
        operations.addAll(getAccountHistory(account, OperationType.MESSAGE));
        operations.addAll(getAccountHistory(account, OperationType.PROPOSAL_CREATE));



        return operations.stream()
                .sorted(Comparator.comparing(o -> o.creationDate))
                .collect(Collectors.toList());
    }

    @Override
    public List<Proposal> getAccountProposals(UserAccount account) {
        return StreamSupport.stream(proposalRepository.findAll().spliterator(), false)
                .peek(op -> op.setApiObjectFactory(apiObjectFactory))
                // hide approved proposal, coz it can't be received in real graphene
//                .filter(proposalH2 -> !proposalH2.approved())
                .filter(
                        proposalH2 -> proposalH2.neededApprovals().contains(account.getId())
                                || proposalH2.getOperation().getAffectedAccounts().contains(account.getId())
                )
                .map(ProposalH2::copyWithoutFeePayer)
                .collect(Collectors.toList());
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
    public UserAccount getAccountById(String name) {
        return createAccount(name);
    }

    @Override
    public List<AssetAmount> getAccountBalances(UserAccount account) {
        return new ArrayList<>(0); //TODO: count by transfers
    }

    @Override
    public Optional<? extends BaseOperation> getLastOperation(UserAccount account) {
        List<? extends BaseOperation> operations = getAccountHistory(account);
        if (operations.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(operations.get(operations.size() - 1));
        }
    }

    @Override
    public List<? extends BaseOperation> operationsAfter(UserAccount account, String operationId) {
        List<? extends BaseOperation> history = getAccountHistory(account);
        ArrayList<BaseOperation> operationAfter = new ArrayList<>(history.size());
        final boolean[] newer = {false};
        if (operationId == null || operationId.isEmpty()){
            newer[0] = true;
        }
        history.forEach(o -> {
            if (newer[0]){
                operationAfter.add(o);
            }
            if(o.getId().equals(operationId)) {
                newer[0] = true;
            }
        });
        return operationAfter;
    }

    @Override
    public void listenAccountUpdatesByUserId(String listenerId, Set<String> accsToListen, Consumer<AccountUpdateObject> onUpdate) {
        accountUpdateListeners.put(listenerId, new AccountUpdateListener<>(accsToListen, onUpdate));
    }

    @Override
    public void removeAccountUpdateListener(String listenerId) {
        accountUpdateListeners.remove(listenerId);
    }

    @Override
    public void listenAccountOperationsByUserId(String listenerId, Set<String> accsToListen, Consumer<ExternalObject> onUpdate) {
        accountOperationListeners.put(listenerId, new AccountUpdateListener<>(accsToListen, onUpdate));
    }

    @Override
    public void removeAccountOperationListener(String listenerId) {
        accountOperationListeners.remove(listenerId);
    }

    private void fireAccountOperation(Set<String> accsToNotify, ExternalObject externalObject) {
        accountOperationListeners.values()
                .stream()
                .filter(listener -> {
                    Set<String> accsToListen = new HashSet<>(listener.accsToListen);
                    accsToListen.retainAll(accsToNotify);
                    return accsToListen.size() > 0;
                })
                .forEach(listener -> listener.fire(externalObject));
    }

    private void fireAccountUpdate(Set<String> accsToNotify) {
        accountUpdateListeners.values()
                .forEach(listener -> accsToNotify.forEach(account -> {
                    if(listener.accsToListen.contains(account)) {
                        UserAccount userAccount = apiObjectFactory.userAccount(account);
                        listener.fire(new AccountUpdateObject() {
                            @Override
                            public int getTotalOperations() {
                                return getAccountHistory(userAccount).size();
                            }

                            @Override
                            public UserAccount getUpdatedAccount() {
                                return userAccount;
                            }
                        });
                    }
                }));
    }

}
