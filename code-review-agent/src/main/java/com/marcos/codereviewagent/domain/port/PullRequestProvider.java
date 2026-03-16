package com.marcos.codereviewagent.domain.port;

import com.marcos.codereviewagent.domain.model.PrDiff;

public interface PullRequestProvider {

    PrDiff fetchPullRequest(String owner, String repo, int prNumber);
    boolean postReview(String owner, String repo, int prNumber, String body);
}
