/*
 * SonarQube PHP Plugin
 * Copyright (C) 2010-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.php.checks;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.php.checks.phpini.PhpIniBoolean;
import org.sonar.php.checks.utils.CheckUtils;
import org.sonar.php.ini.PhpIniCheck;
import org.sonar.php.ini.PhpIniIssue;
import org.sonar.php.ini.tree.PhpIniFile;
import org.sonar.plugins.php.api.tree.declaration.CallArgumentTree;
import org.sonar.plugins.php.api.tree.expression.ExpressionTree;
import org.sonar.plugins.php.api.tree.expression.FunctionCallTree;
import org.sonar.plugins.php.api.visitors.PHPVisitorCheck;

import static org.sonar.php.checks.phpini.PhpIniFiles.checkRequiredBoolean;
import static org.sonar.php.checks.utils.CheckUtils.getLowerCaseFunctionName;

@Rule(key = "S2092")
public class CookiesSecureCheck extends PHPVisitorCheck implements PhpIniCheck {

  private static final String MESSAGE_PHP_INI = "Make sure creating the session cookie without the \"secure\" flag is safe here.";
  private static final String MESSAGE = "Make sure creating this cookie without the \"secure\" flag is safe here.";

  private static final List<String> SET_COOKIE_FUNCTIONS = Arrays.asList("setcookie", "cookiedough");
  private static final int SET_COOKIE_SECURE_PARAMETER = 5;
  private static final String SESSION_COOKIE_FUNC = "session_set_cookie_params";
  private static final int SESSION_COOKIE_SECURE_PARAMETER = 3;

  // Wha ??
  @Override
  public void visitFunctionCall(FunctionCallTree tree) {
    String functionName = getLowerCaseFunctionName(tree);
    Optional<CallArgumentTree> secureArgument;

    if (SET_COOKIE_FUNCTIONS.contains(functionName)) {
      secureArgument = CheckUtils.argument(tree, "secure", SET_COOKIE_SECURE_PARAMETER);
      checkForIssues(tree, secureArgument.orElse(null));
    }
    if (SESSION_COOKIE_FUNC.equals(functionName)) {
      secureArgument = CheckUtils.argument(tree, "secure", SESSION_COOKIE_SECURE_PARAMETER);
      checkForIssues(tree, secureArgument.orElse(null));
    }
    super.visitFunctionCall(tree);
  }

  private void checkForIssues(FunctionCallTree tree, @Nullable CallArgumentTree secureArgument) {
    if (secureArgument != null) {
      raiseIssueIfBadFlag(tree, secureArgument.value());
    } else {
      raiseIssueIfArgumentIsNotDefined(tree);
    }
  }

  private void raiseIssueIfArgumentIsNotDefined(FunctionCallTree tree) {
    if (tree.callArguments().size() == 3) {
      // if only 3 argument are defined there is an ambiguity because of the other constructor, so we don't raise issue
      return;
    }
    context().newIssue(this, tree.callee(), MESSAGE);
  }

  private void raiseIssueIfBadFlag(FunctionCallTree tree, ExpressionTree secureArgument) {
    if (CheckUtils.isFalseValue(secureArgument)) {
      context().newIssue(this, tree.callee(), MESSAGE).secondary(secureArgument, null);
    }
  }
}
