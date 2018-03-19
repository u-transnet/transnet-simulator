package com.github.utransnet.simulator.logging;

import com.github.utransnet.simulator.externalapi.AssetAmount;
import com.github.utransnet.simulator.externalapi.Proposal;
import com.github.utransnet.simulator.externalapi.UserAccount;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.annotation.Configuration;

/**
 * Created by Artem on 15.03.2018.
 */
@Slf4j
@Aspect
@Configuration
public class TransactionLogger {


    //region pointcuts
    // Aspect for annotation on interface doesn.t work, but used for explicit indication in ExternalAPI
    @Pointcut("@annotation(com.github.utransnet.simulator.logging.TransactionPointCut)")
    public void anyTransaction() {

    }

    @Pointcut(value = "execution(* com.github.utransnet.simulator.externalapi.ExternalAPI.sendMessage(..))" +
            " && args(from,to,message)", argNames = "from,to,message")
    private void onSendMessage(UserAccount from, UserAccount to, String message) {

    }

    @Pointcut(value = "execution(* com.github.utransnet.simulator.externalapi.ExternalAPI.sendAsset(..))" +
            " && args(from,to,assetAmount,memo)", argNames = "from,to,assetAmount,memo")
    private void onSendAsset(UserAccount from, UserAccount to, AssetAmount assetAmount, String memo) {

    }

    @Pointcut(value = "execution(* com.github.utransnet.simulator.externalapi.ExternalAPI.sendProposal(..))" +
            " && args(from,to,proposingAccount,feePayer,assetAmount,memo)",
            argNames = "from,to,proposingAccount,feePayer,assetAmount,memo")
    private void onSendProposal(
            UserAccount from,
            UserAccount to,
            UserAccount proposingAccount,
            UserAccount feePayer,
            AssetAmount assetAmount,
            String memo
    ) {

    }

    @Pointcut(value = "execution(* com.github.utransnet.simulator.externalapi.ExternalAPI.approveProposal(..))" +
            " && args(approvingAccount,proposal)", argNames = "approvingAccount,proposal")
    private void onApproveProposal(UserAccount approvingAccount, Proposal proposal) {

    }

    @Pointcut("execution(* com.github.utransnet.simulator.externalapi.ExternalAPI.createAccount(..)) && args(name)")
    private void onCreateAccount(String name) {

    }

    //endregion

    @After(value = "onSendMessage(from,to,message)", argNames = "joinPoint,from,to,message")
    private void logSendMessage(JoinPoint joinPoint, UserAccount from, UserAccount to, String message) {
        log.trace(String.format("%14s: from=<%s>|to=<%s>|message=<%s>", "MESSAGE", from.getName(), to.getName(), message));
    }

    @After(value = "onSendAsset(from, to, assetAmount, memo)", argNames = "joinPoint,from,to,assetAmount,memo")
    private void logSendAsset(
            JoinPoint joinPoint,
            UserAccount from,
            UserAccount to,
            AssetAmount assetAmount,
            String memo
    ) {
        log.trace(String.format(
                "%14s: from=<%s>|to=<%s>|assetAmount=<%s>|memo=<%s>",
                "TRANSFER", from.getName(), to.getName(), assetAmount, memo
        ));
    }

    @After(value = "onSendProposal(from,to,proposingAccount,feePayer,assetAmount,memo)",
            argNames = "joinPoint,from,to,proposingAccount,feePayer,assetAmount,memo")
    private void logSendProposal(
            JoinPoint joinPoint,
            UserAccount from,
            UserAccount to,
            UserAccount proposingAccount,
            UserAccount feePayer,
            AssetAmount assetAmount,
            String memo
    ) {
        log.trace(String.format(
                "%14s: from=<%s>|to=<%s>|assetAmount=<%s>|memo=<%s>|proposingAccount=<%s>|feePayer=<%s>",
                "PROPOSAL",
                from.getName(), to.getName(), assetAmount, memo, proposingAccount.getName(), feePayer.getName()
        ));
    }

    @After(value = "onApproveProposal(approvingAccount,proposal)", argNames = "joinPoint,approvingAccount,proposal")
    private void logApproveProposal(JoinPoint joinPoint, UserAccount approvingAccount, Proposal proposal) {
        log.trace(String.format(
                "%14s: proposalId=<%s>|approveAdded=<%s>|isApproved=<%s>|neededApproves=<%s>",
                "APPROVE_ADDED",
                proposal.getId(), approvingAccount.getName(), proposal.approved(), proposal.neededApproves()
        ));
    }

}
