package com.jraska.livedata.example

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.jraska.livedata.TestLifecycle
import com.jraska.livedata.TestObserver
import com.jraska.livedata.test
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ExampleTest {
  @get:Rule val testRule = InstantTaskExecutorRule()

  private lateinit var viewModel: CounterViewModel

  @Before
  fun before() {
    viewModel = CounterViewModel()
  }

  @Test
  fun directAssertion() {
    viewModel.counterLiveData()
      .test()
      .assertHasValue()
      .assertHistorySize(1)
      .assertNever { it > 0 }

    for (x in 1..4) {
      viewModel.plusButtonClicked()
    }

    viewModel.counterLiveData()
      .test()
      .assertHasValue()
      .assertValue { it > 3 }
      .assertValue(4)
      .assertHistorySize(1) // notice the history size is one since we created new observer
      .assertNever { it > 4 }
  }

  @Test
  fun counterHistoryTest() {
    val testObserver = viewModel.counterLiveData().test()

    testObserver.assertHasValue()
      .assertHistorySize(1)
      .assertNever { it > 0 }

    for (x in 1..4) {
      viewModel.plusButtonClicked()
    }

    testObserver.assertHasValue()
      .assertValue { it > 3 }
      .assertValue(4)
      .assertHistorySize(5)
      .assertValueHistory(0, 1, 2, 3, 4)
      .assertNever { it > 4 }

    for (i in 1..4) {
      viewModel.minusButtonClicked()
    }

    testObserver.assertHasValue()
      .assertHistorySize(9)
      .assertValueHistory(0, 1, 2, 3, 4, 3, 2, 1, 0)
      .assertValue(0)
      .assertNever { it > 4 }
  }

  @Test
  fun usingAssertJ() {
    val testObserver = viewModel.counterLiveData().test()

    for (x in 1..4) {
      viewModel.plusButtonClicked()
    }

    val value = testObserver.value()
    assertThat(value).isEqualTo(4)

    val valueHistory = testObserver.valueHistory()
    assertThat(valueHistory).containsExactly(0, 1, 2, 3, 4)

    viewModel.counterLiveData().removeObserver(testObserver)
    assertThat(viewModel.counterLiveData().hasObservers()).isFalse()
  }

  @Test
  fun useObserverWithLifecycle() {
    val testObserver = TestObserver.create<Int>()
    val testLifecycle = TestLifecycle.initialized()

    viewModel.counterLiveData().observe(testLifecycle, testObserver)

    viewModel.plusButtonClicked()
    viewModel.minusButtonClicked()
    testObserver.assertNoValue()

    testLifecycle.resume()
    for (i in 0..3) {
      viewModel.plusButtonClicked()
    }

    testObserver.assertHasValue()
      .assertValue(4)
      .assertHistorySize(5)
      .assertNever { value -> value > 4 }

    // Potential remove needs to be handled by you
    viewModel.counterLiveData().removeObserver(testObserver)
  }

  @Test
  fun awaitAsyncValue() {
    val testObserver = viewModel.counterLabel()
      .test()
      .assertNoValue()

    viewModel.asyncUpdateLabel("initial")

    testObserver.assertNoValue()
      .awaitValue()
      .assertHasValue()

    viewModel.asyncUpdateLabel("different")

    testObserver
      .map { it.labels.counterLabel }
      .assertValue("initial")
      .awaitNextValue()
      .assertValue("different")
  }
}
