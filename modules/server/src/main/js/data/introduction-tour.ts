import { PopperPlacement } from "shepherd.js";

const steps = [
        {
          id: 'tour-exprs-intro',
          title: "tour_exprs_intro_title",
          text:  "tour_exprs_intro",
          attachTo: {
            element: '.comp-ph-exercise-body',
            on: 'auto' as PopperPlacement,
          },
          buttons: [{text: 'skip'}, { text: 'next' }],
        },
        {
          id: 'tour-exprs-pages',
          title: "tour_exprs_pages_title",
          text:  "tour_exprs_pages",
          attachTo: {
            element: '.pagination',
            on: 'top' as PopperPlacement,
          },
          highlightClass: 'comp-ph-next-question-btn',
          buttons: [{text: 'skip'}, { text: 'next' }],
        },
        {
          id: 'tour-exprs-expr',
          title: "tour_exprs_expr_title",
          text:  "tour_exprs_expr",
          attachTo: {
            element: '.comp-ph-expr',
            on: 'bottom' as PopperPlacement,
          },
          buttons: [{text: 'skip'}, { text: 'next' }],
        },
        {
          id: 'tour-exprs-operator',
          title: "tour_exprs_operator_title",
          text:  "tour_exprs_operator",
          attachTo: {
            element: '.comp-ph-expr-op-btn',
            on: 'bottom' as PopperPlacement,
          },
          buttons: [{text: 'skip'}, { text: 'next' }],
        },
        {
          id: 'tour-exprs-selectedop',
          title: "tour_exprs_selectedop_title",
          text: "tour_exprs_selectedop",
          attachTo: {
            element: '.comp-ph-expr-op-btn.disabled',
            on: 'right' as PopperPlacement,
          },
          buttons: [{text: 'skip'}, { text: 'next' }],
        },
        {
          id: 'tour-exprs-hint',
          title: "tour_exprs_hint_title",
          text: "tour_exprs_hint",
          attachTo: {
            element: '.comp-ph-hint-btn',
            on: 'bottom' as PopperPlacement,
          },
          buttons: [{text: 'skip'}, { text: 'next' }],
        },
        {
          id: 'tour-expr-error-hint',
          title: "tour_exprs_error_hint_title",
          text: "tour_exprs_error_hint",
          attachTo: {
            element: '.comp-ph-feedback-error',
            on: 'top' as PopperPlacement,
          },
          buttons: [{text: 'skip'}, { text: 'next' }],
        },
        {
          id: 'tour-expr-feedback-grade',
          title: "tour_exprs_feedback_grade_title",
          text: "tour_exprs_feedback_grade",
          attachTo: {
            element: '.comp-ph-feedback-grade',
            on: 'top' as PopperPlacement,
          },
          buttons: [{text: 'skip'}, { text: 'next' }],
        },
        {
          id: 'tour-exprs-feedback-steps',
          title: "tour_exprs_feedback_steps_title",
          text: "tour_exprs_feedback_steps",
          attachTo: {
            element: '.comp-ph-feedback-remaining-steps',
            on: 'top' as PopperPlacement,
          },
          buttons: [{text: 'skip'}, { text: 'next' }],
        },
        {
          id: 'tour-exprs-feedback-errors',
          title: "tour_exprs_feedback_errors_title",
          text: "tour_exprs_feedback_errors",
          attachTo: {
            element: '.comp-ph-feedback-error-steps',
            on: 'top' as PopperPlacement,
          },
          buttons: [{text: 'skip'}, { text: 'next' }],
        },
        {
          id: 'tour-exprs-earlyfinish',
          title: "tour_exprs_earlyfinish_title",
          text: "tour_exprs_earlyfinish",
          attachTo: {
            element: '.comp-ph-complete-btn',
            on: 'top' as PopperPlacement,
          },
          buttons: [{text: 'skip'}, { text: 'next' }],
        },
        {
          id: 'tour-exprs-tracevalue',
          title: "tour_exprs_tracevalue_title",
          text: "tour_exprs_tracevalue",
          attachTo: {
            element: '.comp-ph-trace-value',
            on: 'right' as PopperPlacement,
          },
          buttons: [{text: 'skip'}, { text: 'next' }],
        },
];

export default steps;