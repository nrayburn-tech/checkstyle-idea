package org.infernus.idea.checkstyle.importer.modules;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementEntryMatcher;
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementMatchRule;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementAtomMatchCondition;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementCompositeMatchCondition;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementSettingsToken;
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementExtendableSettings;
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementRuleAliasToken;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.infernus.idea.checkstyle.importer.ModuleImporter;
import org.jetbrains.annotations.NotNull;

import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.EntryType.CONSTRUCTOR;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.EntryType.FIELD;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.EntryType.METHOD;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.Modifier.PACKAGE_PRIVATE;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.Modifier.PRIVATE;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.Modifier.PROTECTED;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.Modifier.PUBLIC;
import static com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens.Modifier.STATIC;

@SuppressWarnings("unused")
public class DeclarationOrderImporter extends ModuleImporter {
  private static final Logger LOG = Logger.getInstance(DeclarationOrderImporter.class);

  private static final String IGNORE_CONSTRUCTORS = "ignoreConstructors";
  private static final String IGNORE_MODIFIERS = "ignoreModifiers";

  private boolean ignoreConstructors;
  private boolean ignoreModifiers;

  @Override
  protected void handleAttribute(@NotNull String attrName, @NotNull String attrValue) {
    switch (attrName) {
      case IGNORE_CONSTRUCTORS:
        ignoreConstructors = Boolean.parseBoolean(attrValue);
        break;
      case IGNORE_MODIFIERS:
        ignoreModifiers = Boolean.parseBoolean(attrValue);
        break;
      default:
        LOG.warn("Unexpected declaration order import policy: " + attrValue);
        break;
    }
  }

  @Override
  public void importTo(@NotNull CodeStyleSettings settings) {
    ArrangementSettingsToken[] modifiers = {PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE};

    final List<StdArrangementMatchRule> matchRules = new ArrayList<>();
    if (ignoreModifiers) {
      matchRules.add(createRule(FIELD, STATIC));
      matchRules.add(createRule(FIELD));
    } else {
      for (ArrangementSettingsToken modifier : modifiers) {
        matchRules.add(createRule(FIELD, STATIC, modifier));
      }
      for (ArrangementSettingsToken modifier : modifiers) {
        matchRules.add(createRule(FIELD, modifier));
      }
    }

    if (!ignoreConstructors) {
      matchRules.add(createRule(CONSTRUCTOR));
    }

    matchRules.add(createRule(METHOD));

    StdArrangementRuleAliasToken visibilityAlias =
        new StdArrangementRuleAliasToken(
            "visibility",
            List.of(
                createRule(PUBLIC),
                createRule(PROTECTED),
                createRule(PACKAGE_PRIVATE),
                createRule(PRIVATE)));
    StdArrangementExtendableSettings arrangementSettings =
        StdArrangementExtendableSettings.createByMatchRules(
            Collections.emptyList(), matchRules, List.of(visibilityAlias));
    settings.setArrangementSettings(arrangementSettings);
  }

  private static StdArrangementMatchRule createRule(ArrangementSettingsToken... conditions) {
    if (conditions.length == 1) {
      return new StdArrangementMatchRule(
          new StdArrangementEntryMatcher(new ArrangementAtomMatchCondition(conditions[0])));
    }

    ArrangementCompositeMatchCondition composite = new ArrangementCompositeMatchCondition();
    for (ArrangementSettingsToken condition : conditions) {
      composite.addOperand(new ArrangementAtomMatchCondition(condition));
    }
    return new StdArrangementMatchRule(new StdArrangementEntryMatcher(composite));
  }
}
