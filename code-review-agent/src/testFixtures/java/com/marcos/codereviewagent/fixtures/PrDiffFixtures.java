package com.marcos.codereviewagent.fixtures;

import com.marcos.codereviewagent.domain.model.FileChange;
import com.marcos.codereviewagent.domain.model.PrDiff;
import java.util.List;

public final class PrDiffFixtures {

    private PrDiffFixtures() {}

    public static FileChange aModifiedJavaFile() {
        return FileChange.builder()
                .filename("src/main/java/com/example/Service.java")
                .status("modified")
                .additions(10)
                .deletions(3)
                .patch("@@ -1,5 +1,12 @@\n+import java.util.Optional;\n code...")
                .build();
    }

    public static FileChange anAddedTestFile() {
        return FileChange.builder()
                .filename("src/test/java/com/example/ServiceTest.java")
                .status("added")
                .additions(45)
                .deletions(0)
                .patch("@@ -0,0 +1,45 @@\n+package com.example;\n+...")
                .build();
    }

    public static PrDiff aPrDiff() {
        return PrDiff.builder()
                .title("Fix null pointer in payment processing")
                .description("Handles the case where merchant config is absent")
                .baseBranch("main")
                .headBranch("fix/npe-merchant-config")
                .diffContent("full diff content here")
                .files(List.of(aModifiedJavaFile(), anAddedTestFile()))
                .build();
    }
}