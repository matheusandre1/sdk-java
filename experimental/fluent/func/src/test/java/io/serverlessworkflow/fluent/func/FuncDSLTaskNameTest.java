/*
 * Copyright 2020-Present The Serverless Workflow Specification Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.serverlessworkflow.fluent.func;

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.consume;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.forEach;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.forEachItem;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.function;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.switchWhen;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.switchWhenOrElse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.api.types.SwitchCase;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.api.types.func.LoopFunction;
import io.serverlessworkflow.api.types.func.SwitchCasePredicate;
import io.serverlessworkflow.fluent.func.configurers.FuncTaskConfigurer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("FuncDSL — named control-flow overloads")
class FuncDSLTaskNameTest {

  private static Workflow buildWorkflow(FuncTaskConfigurer... steps) {
    return FuncWorkflowBuilder.workflow("taskNameTest").tasks(steps).build();
  }

  private static List<TaskItem> buildItems(FuncTaskConfigurer... steps) {
    return buildWorkflow(steps).getDo();
  }

  @Nested
  @DisplayName("switchWhen — Predicate overload")
  class SwitchWhenPredicate {

    @Test
    @DisplayName("unnamed switchWhen produces auto-generated name starting with 'switch-'")
    void unnamed_producesAutoName() {
      List<TaskItem> items =
          buildItems(switchWhen((Integer v) -> v > 0, "positive", Integer.class));
      assertEquals(1, items.size());
      assertTrue(
          items.get(0).getName().startsWith("switch-"),
          "Unnamed switchWhen should auto-generate name starting with 'switch-'");
      assertNotNull(items.get(0).getTask().getSwitchTask());
    }

    @Test
    @DisplayName("named switchWhen uses the provided taskName")
    void named_usesProvidedName() {
      List<TaskItem> items =
          buildItems(switchWhen("checkSign", (Integer v) -> v > 0, "positive", Integer.class));
      assertEquals(1, items.size());
      assertEquals("checkSign", items.get(0).getName());
      assertNotNull(items.get(0).getTask().getSwitchTask());
    }

    @Test
    @DisplayName("named switchWhen stores predicate in SwitchCasePredicate")
    void named_storesPredicateInCase() {
      List<TaskItem> items =
          buildItems(switchWhen("isPositive", (Integer v) -> v > 0, "positive", Integer.class));
      Task switchTask = items.get(0).getTask();
      var cases = switchTask.getSwitchTask().getSwitch();
      assertEquals(1, cases.size());
      SwitchCase sc = cases.get(0).getSwitchCase();
      assertInstanceOf(
          SwitchCasePredicate.class, sc, "Predicate-based case should be SwitchCasePredicate");
      assertNotNull(
          ((SwitchCasePredicate) sc).predicate(),
          "Predicate should be stored in the predicate field");
      assertEquals("positive", sc.getThen().getString());
    }

    @Test
    @DisplayName("multiple unnamed switchWhen produce sequential auto-names")
    void multipleUnnamed_produceSequentialNames() {
      List<TaskItem> items =
          buildItems(
              switchWhen((Integer v) -> v > 0, "pos", Integer.class),
              switchWhen((String s) -> s.isEmpty(), "empty", String.class));
      assertEquals(2, items.size());
      assertTrue(items.get(0).getName().startsWith("switch-"));
      assertTrue(items.get(1).getName().startsWith("switch-"));
    }

    @Test
    @DisplayName("multiple named switchWhen use distinct provided names")
    void multipleNamed_useDistinctNames() {
      List<TaskItem> items =
          buildItems(
              switchWhen("checkPositive", (Integer v) -> v > 0, "pos", Integer.class),
              switchWhen("checkEmpty", (String s) -> s.isEmpty(), "empty", String.class));
      assertEquals("checkPositive", items.get(0).getName());
      assertEquals("checkEmpty", items.get(1).getName());
    }
  }

  @Nested
  @DisplayName("switchWhen — JQ expression overload")
  class SwitchWhenJq {

    @Test
    @DisplayName("unnamed JQ switchWhen produces auto-generated name")
    void unnamed_producesAutoName() {
      List<TaskItem> items = buildItems(switchWhen(".approved == true", "approveOrder"));
      assertEquals(1, items.size());
      assertTrue(items.get(0).getName().startsWith("switch-"));
      assertNotNull(items.get(0).getTask().getSwitchTask());
    }

    @Test
    @DisplayName("named JQ switchWhen uses the provided taskName")
    void named_usesProvidedName() {
      List<TaskItem> items =
          buildItems(switchWhen("approvalGate", ".approved == true", "approveOrder"));
      assertEquals(1, items.size());
      assertEquals("approvalGate", items.get(0).getName());
    }

    @Test
    @DisplayName("named JQ switchWhen configures switch case with JQ expression")
    void named_configuresJqExpression() {
      List<TaskItem> items =
          buildItems(switchWhen("checkApproval", ".approved == true", "approve"));
      Task switchTask = items.get(0).getTask();
      var cases = switchTask.getSwitchTask().getSwitch();
      assertEquals(1, cases.size());
      assertEquals(".approved == true", cases.get(0).getSwitchCase().getWhen());
    }

    @Test
    @DisplayName("unnamed JQ switchWhen with null expression builds without validation")
    void unnamed_nullJqExpression_buildsWithNull() {
      List<TaskItem> items = buildItems(switchWhen((String) null, "approve"));
      assertEquals(1, items.size());
    }

    @Test
    @DisplayName("unnamed JQ switchWhen with null thenTask builds without validation")
    void unnamed_nullThenTask_buildsWithNull() {
      List<TaskItem> items = buildItems(switchWhen(".x", (String) null));
      assertEquals(1, items.size());
    }

    @Test
    @DisplayName("named JQ switchWhen with null jqExpression builds without validation")
    void named_nullJqExpression_buildsWithNull() {
      List<TaskItem> items = buildItems(switchWhen("gate", (String) null, "approve"));
      assertEquals(1, items.size());
    }

    @Test
    @DisplayName("named JQ switchWhen with null thenTask builds without validation")
    void named_nullThenTask_buildsWithNull() {
      List<TaskItem> items = buildItems(switchWhen("gate", ".x", (String) null));
      assertEquals(1, items.size());
    }
  }

  @Nested
  @DisplayName("switchWhenOrElse — Predicate + FlowDirectiveEnum overload")
  class SwitchWhenOrElsePredicateDirective {

    @Test
    @DisplayName("unnamed produces auto-generated name")
    void unnamed_producesAutoName() {
      List<TaskItem> items =
          buildItems(
              switchWhenOrElse(
                  (Integer v) -> v > 0, "positive", FlowDirectiveEnum.END, Integer.class));
      assertEquals(1, items.size());
      assertTrue(items.get(0).getName().startsWith("switch-"));
    }

    @Test
    @DisplayName("named uses the provided taskName")
    void named_usesProvidedName() {
      List<TaskItem> items =
          buildItems(
              switchWhenOrElse(
                  "checkSign",
                  (Integer v) -> v > 0,
                  "positive",
                  FlowDirectiveEnum.END,
                  Integer.class));
      assertEquals("checkSign", items.get(0).getName());
    }

    @Test
    @DisplayName("named configures switch with predicate case and default directive")
    void named_configuresCaseAndDefaultDirective() {
      List<TaskItem> items =
          buildItems(
              switchWhenOrElse(
                  "scoreGate",
                  (Integer v) -> v >= 80,
                  "pass",
                  FlowDirectiveEnum.END,
                  Integer.class));
      Task switchTask = items.get(0).getTask();
      var cases = switchTask.getSwitchTask().getSwitch();
      assertEquals(2, cases.size());
      SwitchCase case0 = cases.get(0).getSwitchCase();
      assertInstanceOf(
          SwitchCasePredicate.class, case0, "Predicate case should be SwitchCasePredicate");
      assertNotNull(((SwitchCasePredicate) case0).predicate());
      assertEquals("pass", case0.getThen().getString());
      assertEquals(
          FlowDirectiveEnum.END, cases.get(1).getSwitchCase().getThen().getFlowDirectiveEnum());
    }
  }

  @Nested
  @DisplayName("switchWhenOrElse — SerializablePredicate + FlowDirectiveEnum overload")
  class SwitchWhenOrElseSerializableDirective {

    @Test
    @DisplayName("unnamed produces auto-generated name")
    void unnamed_producesAutoName() {
      List<TaskItem> items =
          buildItems(switchWhenOrElse((Integer v) -> v > 0, "positive", FlowDirectiveEnum.END));
      assertEquals(1, items.size());
      assertTrue(items.get(0).getName().startsWith("switch-"));
    }

    @Test
    @DisplayName("named uses the provided taskName")
    void named_usesProvidedName() {
      List<TaskItem> items =
          buildItems(
              switchWhenOrElse(
                  "signCheck", (Integer v) -> v > 0, "positive", FlowDirectiveEnum.END));
      assertEquals("signCheck", items.get(0).getName());
    }

    @Test
    @DisplayName("named configures predicate case and default directive")
    void named_configuresPredicateAndDirective() {
      List<TaskItem> items =
          buildItems(
              switchWhenOrElse(
                  "signCheck", (Integer v) -> v > 0, "positive", FlowDirectiveEnum.END));
      Task switchTask = items.get(0).getTask();
      var cases = switchTask.getSwitchTask().getSwitch();
      assertEquals(2, cases.size());
      assertInstanceOf(SwitchCasePredicate.class, cases.get(0).getSwitchCase());
      assertNotNull(((SwitchCasePredicate) cases.get(0).getSwitchCase()).predicate());
      assertEquals(
          FlowDirectiveEnum.END, cases.get(1).getSwitchCase().getThen().getFlowDirectiveEnum());
    }
  }

  @Nested
  @DisplayName("switchWhenOrElse — Predicate + orElse task name overload")
  class SwitchWhenOrElsePredicateTask {

    @Test
    @DisplayName("unnamed produces auto-generated name")
    void unnamed_producesAutoName() {
      List<TaskItem> items =
          buildItems(switchWhenOrElse((Integer v) -> v > 0, "positive", "negative", Integer.class));
      assertEquals(1, items.size());
      assertTrue(items.get(0).getName().startsWith("switch-"));
    }

    @Test
    @DisplayName("named uses the provided taskName")
    void named_usesProvidedName() {
      List<TaskItem> items =
          buildItems(
              switchWhenOrElse(
                  "validateSign", (Integer v) -> v > 0, "positive", "negative", Integer.class));
      assertEquals("validateSign", items.get(0).getName());
    }

    @Test
    @DisplayName("named configures predicate case and orElse task target")
    void named_configuresPredicateAndOrElseTask() {
      List<TaskItem> items =
          buildItems(
              switchWhenOrElse(
                  "dataCheck", (Integer v) -> v > 0, "positive", "negative", Integer.class));
      Task switchTask = items.get(0).getTask();
      var cases = switchTask.getSwitchTask().getSwitch();
      assertEquals(2, cases.size());
      SwitchCase case0 = cases.get(0).getSwitchCase();
      assertInstanceOf(
          SwitchCasePredicate.class, case0, "Predicate case should be SwitchCasePredicate");
      assertNotNull(((SwitchCasePredicate) case0).predicate());
      assertEquals("positive", case0.getThen().getString());
      assertEquals("negative", cases.get(1).getSwitchCase().getThen().getString());
    }

    @Test
    @DisplayName("named switchWhenOrElse integrates with .then() navigation — no guessing")
    void named_enablesThenNavigationWithoutGuessing() {
      Workflow wf =
          buildWorkflow(
              function("getData", (Integer v) -> v, Integer.class),
              switchWhenOrElse(
                  "validateData",
                  (Integer v) -> v > 0,
                  "processValid",
                  "handleInvalid",
                  Integer.class),
              consume("processValid", (Integer v) -> {}, Integer.class),
              consume("handleInvalid", (Integer v) -> {}, Integer.class));

      List<TaskItem> items = wf.getDo();
      assertEquals(
          "validateData",
          items.get(1).getName(),
          "The switch task should use the explicit name for .then() navigation");
    }
  }

  @Nested
  @DisplayName("switchWhenOrElse — SerializablePredicate + orElse task name overload")
  class SwitchWhenOrElseSerializableTask {

    @Test
    @DisplayName("unnamed produces auto-generated name")
    void unnamed_producesAutoName() {
      List<TaskItem> items =
          buildItems(switchWhenOrElse((Integer v) -> v > 0, "positive", "negative"));
      assertEquals(1, items.size());
      assertTrue(items.get(0).getName().startsWith("switch-"));
    }

    @Test
    @DisplayName("named uses the provided taskName")
    void named_usesProvidedName() {
      List<TaskItem> items =
          buildItems(switchWhenOrElse("signGate", (Integer v) -> v > 0, "positive", "negative"));
      assertEquals("signGate", items.get(0).getName());
    }

    @Test
    @DisplayName("named configures both when and orElse targets")
    void named_configuresBothTargets() {
      List<TaskItem> items =
          buildItems(switchWhenOrElse("signGate", (Integer v) -> v > 0, "positive", "negative"));
      var cases = items.get(0).getTask().getSwitchTask().getSwitch();
      assertEquals(2, cases.size());
      assertEquals("positive", cases.get(0).getSwitchCase().getThen().getString());
      assertEquals("negative", cases.get(1).getSwitchCase().getThen().getString());
    }
  }

  @Nested
  @DisplayName("switchWhenOrElse — JQ + FlowDirectiveEnum overload")
  class SwitchWhenOrElseJqDirective {

    @Test
    @DisplayName("unnamed produces auto-generated name")
    void unnamed_producesAutoName() {
      List<TaskItem> items =
          buildItems(switchWhenOrElse(".score >= 80", "pass", FlowDirectiveEnum.END));
      assertEquals(1, items.size());
      assertTrue(items.get(0).getName().startsWith("switch-"));
    }

    @Test
    @DisplayName("named uses the provided taskName")
    void named_usesProvidedName() {
      List<TaskItem> items =
          buildItems(switchWhenOrElse("scoreGate", ".score >= 80", "pass", FlowDirectiveEnum.END));
      assertEquals("scoreGate", items.get(0).getName());
    }

    @Test
    @DisplayName("named configures JQ condition and default directive")
    void named_configuresJqAndDirective() {
      List<TaskItem> items =
          buildItems(switchWhenOrElse("examGate", ".score >= 80", "pass", FlowDirectiveEnum.END));
      Task switchTask = items.get(0).getTask();
      var cases = switchTask.getSwitchTask().getSwitch();
      assertEquals(2, cases.size());
      assertEquals(".score >= 80", cases.get(0).getSwitchCase().getWhen());
      assertEquals(
          FlowDirectiveEnum.END, cases.get(1).getSwitchCase().getThen().getFlowDirectiveEnum());
    }

    @Test
    @DisplayName("unnamed with null jqExpression throws NPE at creation time")
    void unnamed_nullJq_throwsNPE() {
      assertThrows(
          NullPointerException.class,
          () -> switchWhenOrElse((String) null, "pass", FlowDirectiveEnum.END));
    }

    @Test
    @DisplayName("named with null jqExpression throws NPE at creation time")
    void named_nullJq_throwsNPE() {
      assertThrows(
          NullPointerException.class,
          () -> switchWhenOrElse("gate", (String) null, "pass", FlowDirectiveEnum.END));
    }

    @Test
    @DisplayName("named with null thenTask throws NPE at creation time")
    void named_nullThenTask_throwsNPE() {
      assertThrows(
          NullPointerException.class,
          () -> switchWhenOrElse("gate", ".x", (String) null, FlowDirectiveEnum.END));
    }

    @Test
    @DisplayName("named with null otherwise directive throws NPE at creation time")
    void named_nullDirective_throwsNPE() {
      assertThrows(
          NullPointerException.class,
          () -> switchWhenOrElse("gate", ".x", "pass", (FlowDirectiveEnum) null));
    }
  }

  @Nested
  @DisplayName("switchWhenOrElse — JQ + orElse task name overload")
  class SwitchWhenOrElseJqTask {

    @Test
    @DisplayName("unnamed produces auto-generated name")
    void unnamed_producesAutoName() {
      List<TaskItem> items = buildItems(switchWhenOrElse(".approved", "send", "draft"));
      assertEquals(1, items.size());
      assertTrue(items.get(0).getName().startsWith("switch-"));
    }

    @Test
    @DisplayName("named uses the provided taskName")
    void named_usesProvidedName() {
      List<TaskItem> items =
          buildItems(switchWhenOrElse("approvalGate", ".approved", "send", "draft"));
      assertEquals("approvalGate", items.get(0).getName());
    }

    @Test
    @DisplayName("named configures JQ condition and both targets")
    void named_configuresJqAndBothTargets() {
      List<TaskItem> items =
          buildItems(switchWhenOrElse("reviewGate", ".approved", "send", "draft"));
      Task switchTask = items.get(0).getTask();
      var cases = switchTask.getSwitchTask().getSwitch();
      assertEquals(2, cases.size());
      assertEquals(".approved", cases.get(0).getSwitchCase().getWhen());
      assertEquals("draft", cases.get(1).getSwitchCase().getThen().getString());
    }

    @Test
    @DisplayName("unnamed with null jqExpression throws NPE at creation time")
    void unnamed_nullJq_throwsNPE() {
      assertThrows(
          NullPointerException.class, () -> switchWhenOrElse((String) null, "send", "draft"));
    }

    @Test
    @DisplayName("named with null jqExpression throws NPE at creation time")
    void named_nullJq_throwsNPE() {
      assertThrows(
          NullPointerException.class,
          () -> switchWhenOrElse("gate", (String) null, "send", "draft"));
    }

    @Test
    @DisplayName("named with null thenTask throws NPE at creation time")
    void named_nullThenTask_throwsNPE() {
      assertThrows(
          NullPointerException.class, () -> switchWhenOrElse("gate", ".x", (String) null, "draft"));
    }

    @Test
    @DisplayName("named with null otherwiseTask throws NPE at creation time")
    void named_nullOtherwiseTask_throwsNPE() {
      assertThrows(
          NullPointerException.class, () -> switchWhenOrElse("gate", ".x", "send", (String) null));
    }
  }

  @Nested
  @DisplayName("forEach — SerializableFunction + body overload")
  class ForEachSerializableFunctionBody {

    @Test
    @DisplayName("unnamed produces auto-generated name starting with 'for-'")
    void unnamed_producesAutoName() {
      List<TaskItem> items = buildItems(forEach((String s) -> List.of(s.split(",")), tb -> {}));
      assertEquals(1, items.size());
      assertTrue(
          items.get(0).getName().startsWith("for-"),
          "Unnamed forEach should auto-generate name starting with 'for-'");
      assertNotNull(items.get(0).getTask().getForTask());
    }

    @Test
    @DisplayName("named uses the provided taskName")
    void named_usesProvidedName() {
      List<TaskItem> items =
          buildItems(forEach("splitItems", (String s) -> List.of(s.split(",")), tb -> {}));
      assertEquals("splitItems", items.get(0).getName());
      assertNotNull(items.get(0).getTask().getForTask());
    }

    @Test
    @DisplayName("named forEach with inner body tasks")
    void named_withInnerBody() {
      List<TaskItem> items =
          buildItems(
              forEach(
                  "processLines",
                  (String s) -> List.of(s.split(",")),
                  tb -> tb.set("transform", "$.x = $.x + 1")));
      assertEquals("processLines", items.get(0).getName());
      assertNotNull(items.get(0).getTask().getForTask());
      List<TaskItem> innerItems = items.get(0).getTask().getForTask().getDo();
      assertNotNull(innerItems);
      assertEquals(1, innerItems.size());
    }
  }

  @Nested
  @DisplayName("forEach — SerializableFunction + LoopFunction overload")
  class ForEachSerializableFunctionLoop {

    @Test
    @DisplayName("unnamed produces auto-generated name")
    void unnamed_producesAutoName() {
      LoopFunction<String, String, Object> loopFn = (ctx, item) -> ctx;
      List<TaskItem> items = buildItems(forEach((String s) -> List.of(s.split(",")), loopFn));
      assertEquals(1, items.size());
      assertTrue(items.get(0).getName().startsWith("for-"));
    }

    @Test
    @DisplayName("named uses the provided taskName")
    void named_usesProvidedName() {
      LoopFunction<String, String, Object> loopFn = (ctx, item) -> ctx;
      List<TaskItem> items =
          buildItems(forEach("mapItems", (String s) -> List.of(s.split(",")), loopFn));
      assertEquals("mapItems", items.get(0).getName());
      assertNotNull(items.get(0).getTask().getForTask());
    }
  }

  @Nested
  @DisplayName("forEachItem — SerializableFunction + Function overload")
  class ForEachItemOverload {

    @Test
    @DisplayName("unnamed produces auto-generated name")
    void unnamed_producesAutoName() {
      List<TaskItem> items =
          buildItems(forEachItem((String s) -> List.of(s.split(",")), (String item) -> item));
      assertEquals(1, items.size());
      assertTrue(items.get(0).getName().startsWith("for-"));
    }

    @Test
    @DisplayName("named uses the provided taskName")
    void named_usesProvidedName() {
      List<TaskItem> items =
          buildItems(
              forEachItem(
                  "transformEach", (String s) -> List.of(s.split(",")), (String item) -> item));
      assertEquals("transformEach", items.get(0).getName());
      assertNotNull(items.get(0).getTask().getForTask());
    }
  }

  @Nested
  @DisplayName("forEach — Collection + body overload")
  class ForEachCollectionBody {

    @Test
    @DisplayName("unnamed produces auto-generated name")
    void unnamed_producesAutoName() {
      Collection<String> col = List.of("a", "b", "c");
      List<TaskItem> items = buildItems(forEach(col, tb -> {}));
      assertEquals(1, items.size());
      assertTrue(items.get(0).getName().startsWith("for-"));
    }

    @Test
    @DisplayName("named uses the provided taskName")
    void named_usesProvidedName() {
      Collection<String> col = List.of("a", "b", "c");
      List<TaskItem> items = buildItems(forEach("iterateItems", col, tb -> {}));
      assertEquals("iterateItems", items.get(0).getName());
      assertNotNull(items.get(0).getTask().getForTask());
    }

    @Test
    @DisplayName("named with empty collection builds successfully")
    void named_emptyCollection_buildsSuccessfully() {
      Collection<String> col = List.of();
      List<TaskItem> items = buildItems(forEach("emptyLoop", col, tb -> {}));
      assertEquals("emptyLoop", items.get(0).getName());
      assertNotNull(items.get(0).getTask().getForTask());
    }
  }

  @Nested
  @DisplayName("forEach — List + body overload")
  class ForEachListBody {

    @Test
    @DisplayName("unnamed produces auto-generated name")
    void unnamed_producesAutoName() {
      List<String> list = List.of("x", "y");
      List<TaskItem> items = buildItems(forEach(list, tb -> {}));
      assertEquals(1, items.size());
      assertTrue(items.get(0).getName().startsWith("for-"));
    }

    @Test
    @DisplayName("named uses the provided taskName")
    void named_usesProvidedName() {
      List<String> list = List.of("x", "y");
      List<TaskItem> items = buildItems(forEach("iterateList", list, tb -> {}));
      assertEquals("iterateList", items.get(0).getName());
      assertNotNull(items.get(0).getTask().getForTask());
    }

    @Test
    @DisplayName("named with ArrayList (mutable list) builds successfully")
    void named_mutableList_buildsSuccessfully() {
      ArrayList<String> list = new ArrayList<>(List.of("a", "b"));
      List<TaskItem> items = buildItems(forEach("processMutable", list, tb -> {}));
      assertEquals("processMutable", items.get(0).getName());
    }
  }

  @Nested
  @DisplayName("Backward compatibility — unnamed overloads still work")
  class BackwardCompatibility {

    @Test
    @DisplayName("unnamed switchWhen(Predicate, thenTask, Class) still works")
    void unnamedSwitchWhenPredicate() {
      List<TaskItem> items = buildItems(switchWhen((Integer v) -> v > 0, "pos", Integer.class));
      assertEquals(1, items.size());
      assertNotNull(items.get(0).getTask().getSwitchTask());
    }

    @Test
    @DisplayName("unnamed switchWhen(JQ, thenTask) still works")
    void unnamedSwitchWhenJq() {
      List<TaskItem> items = buildItems(switchWhen(".approved", "approve"));
      assertEquals(1, items.size());
      assertNotNull(items.get(0).getTask().getSwitchTask());
    }

    @Test
    @DisplayName("unnamed switchWhenOrElse(Predicate, thenTask, directive, Class) still works")
    void unnamedSwitchWhenOrElseDirective() {
      List<TaskItem> items =
          buildItems(
              switchWhenOrElse((Integer v) -> v > 0, "pos", FlowDirectiveEnum.END, Integer.class));
      assertEquals(1, items.size());
      assertNotNull(items.get(0).getTask().getSwitchTask());
    }

    @Test
    @DisplayName("unnamed switchWhenOrElse(SerializablePredicate, thenTask, directive) still works")
    void unnamedSwitchWhenOrElseSerializableDirective() {
      List<TaskItem> items =
          buildItems(switchWhenOrElse((Integer v) -> v > 0, "pos", FlowDirectiveEnum.END));
      assertEquals(1, items.size());
    }

    @Test
    @DisplayName("unnamed switchWhenOrElse(Predicate, thenTask, orElseTask, Class) still works")
    void unnamedSwitchWhenOrElseTask() {
      List<TaskItem> items =
          buildItems(switchWhenOrElse((Integer v) -> v > 0, "pos", "neg", Integer.class));
      assertEquals(1, items.size());
    }

    @Test
    @DisplayName(
        "unnamed switchWhenOrElse(SerializablePredicate, thenTask, orElseTask) still works")
    void unnamedSwitchWhenOrElseSerializableTask() {
      List<TaskItem> items = buildItems(switchWhenOrElse((Integer v) -> v > 0, "pos", "neg"));
      assertEquals(1, items.size());
    }

    @Test
    @DisplayName("unnamed switchWhenOrElse(JQ, thenTask, directive) still works")
    void unnamedSwitchWhenOrElseJqDirective() {
      List<TaskItem> items =
          buildItems(switchWhenOrElse(".approved", "send", FlowDirectiveEnum.END));
      assertEquals(1, items.size());
    }

    @Test
    @DisplayName("unnamed switchWhenOrElse(JQ, thenTask, orElseTask) still works")
    void unnamedSwitchWhenOrElseJqTask() {
      List<TaskItem> items = buildItems(switchWhenOrElse(".approved", "send", "draft"));
      assertEquals(1, items.size());
    }

    @Test
    @DisplayName("unnamed forEach(SerializableFunction, body) still works")
    void unnamedForEachFunction() {
      List<TaskItem> items = buildItems(forEach((String s) -> List.of(s), tb -> {}));
      assertEquals(1, items.size());
    }

    @Test
    @DisplayName("unnamed forEach(SerializableFunction, LoopFunction) still works")
    void unnamedForEachLoop() {
      LoopFunction<String, String, Object> loopFn = (ctx, item) -> ctx;
      List<TaskItem> items = buildItems(forEach((String s) -> List.of(s), loopFn));
      assertEquals(1, items.size());
    }

    @Test
    @DisplayName("unnamed forEachItem(SerializableFunction, Function) still works")
    void unnamedForEachItem() {
      List<TaskItem> items =
          buildItems(forEachItem((String s) -> List.of(s), (String item) -> item));
      assertEquals(1, items.size());
    }

    @Test
    @DisplayName("unnamed forEach(Collection, body) still works")
    void unnamedForEachCollection() {
      List<TaskItem> items = buildItems(forEach(List.of("a"), tb -> {}));
      assertEquals(1, items.size());
    }

    @Test
    @DisplayName("unnamed forEach(List, body) still works")
    void unnamedForEachList() {
      List<String> list = List.of("a", "b");
      List<TaskItem> items = buildItems(forEach(list, tb -> {}));
      assertEquals(1, items.size());
    }
  }

  @Nested
  @DisplayName("Integration — named control flow in realistic workflow")
  class Integration {

    @Test
    @DisplayName("named switchWhenOrElse + .then() navigation — end-to-end")
    void namedSwitchWithThenNavigation() {
      Workflow wf =
          buildWorkflow(
              function("loadData", (String s) -> s, String.class),
              switchWhenOrElse(
                  "validateData",
                  (String s) -> !s.isEmpty(),
                  "processValid",
                  "handleInvalid",
                  String.class),
              consume("processValid", (String s) -> {}, String.class),
              consume("handleInvalid", (String s) -> {}, String.class));

      List<TaskItem> items = wf.getDo();
      assertEquals(4, items.size());
      assertEquals("loadData", items.get(0).getName());
      assertEquals("validateData", items.get(1).getName());
      assertEquals("processValid", items.get(2).getName());
      assertEquals("handleInvalid", items.get(3).getName());
    }

    @Test
    @DisplayName("named forEach in workflow with descriptive name")
    void namedForEachInWorkflow() {
      Workflow wf =
          buildWorkflow(
              function("getData", (String s) -> s, String.class),
              forEach(
                  "processEachItem",
                  (String s) -> List.of(s.split(",")),
                  tb -> tb.set("transform", "$.x = 1")));

      List<TaskItem> items = wf.getDo();
      assertEquals(2, items.size());
      assertEquals("getData", items.get(0).getName());
      assertEquals("processEachItem", items.get(1).getName());
    }

    @Test
    @DisplayName("mixed named and unnamed control flow tasks")
    void mixedNamedAndUnnamed() {
      List<TaskItem> items =
          buildItems(
              switchWhen("firstGate", (Integer v) -> v > 0, "pos", Integer.class),
              switchWhen((String s) -> s.isEmpty(), "empty", String.class),
              forEach("namedLoop", List.of(1, 2), tb -> {}),
              forEach(List.of("x"), tb -> {}));

      assertEquals("firstGate", items.get(0).getName());
      assertTrue(items.get(1).getName().startsWith("switch-"));
      assertEquals("namedLoop", items.get(2).getName());
      assertTrue(items.get(3).getName().startsWith("for-"));
    }

    @Test
    @DisplayName("named JQ switchWhenOrElse integrates with workflow navigation")
    void namedJqSwitchIntegration() {
      Workflow wf =
          buildWorkflow(
              function("fetchOrder", (String s) -> s, String.class),
              switchWhenOrElse("checkApproval", ".approved == true", "fulfillOrder", "rejectOrder"),
              consume("fulfillOrder", (String s) -> {}, String.class),
              consume("rejectOrder", (String s) -> {}, String.class));

      List<TaskItem> items = wf.getDo();
      assertEquals("checkApproval", items.get(1).getName());
      Task switchTask = items.get(1).getTask();
      assertNotNull(switchTask.getSwitchTask());
      var cases = switchTask.getSwitchTask().getSwitch();
      assertEquals(2, cases.size());
      assertEquals(".approved == true", cases.get(0).getSwitchCase().getWhen());
      assertEquals("rejectOrder", cases.get(1).getSwitchCase().getThen().getString());
    }
  }
}
