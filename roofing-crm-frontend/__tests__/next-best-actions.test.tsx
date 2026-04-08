import React from "react";
import { render, screen } from "./test-utils";
import { NextBestActions } from "@/components/NextBestActions";

describe("NextBestActions", () => {
  it("renders lead NEW action set with Call customer linking to customer when customerId is present", () => {
    render(
      <NextBestActions entityType="lead" status="NEW" leadId="lead-1" customerId="cust-1" />
    );

    expect(screen.getByRole("link", { name: /call customer/i })).toHaveAttribute(
      "href",
      "/app/customers/cust-1"
    );
    expect(screen.getByRole("link", { name: /schedule inspection/i })).toHaveAttribute(
      "href",
      "/app/tasks/new?leadId=lead-1"
    );
    expect(screen.getByRole("link", { name: /convert to job/i })).toHaveAttribute(
      "href",
      "/app/leads/lead-1/convert"
    );
  });

  it("renders Convert to job first when convertible NEW lead", () => {
    render(
      <NextBestActions entityType="lead" status="NEW" leadId="lead-1" customerId="cust-1" />
    );
    const convert = screen.getByRole("link", { name: /convert to job/i });
    const call = screen.getByRole("link", { name: /call customer/i });
    const schedule = screen.getByRole("link", { name: /schedule inspection/i });
    expect(convert.compareDocumentPosition(call) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(call.compareDocumentPosition(schedule) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it("renders lead NEW Call customer linking to lead detail when customerId is absent", () => {
    render(<NextBestActions entityType="lead" status="NEW" leadId="lead-1" />);

    expect(screen.getByRole("link", { name: /call customer/i })).toHaveAttribute(
      "href",
      "/app/leads/lead-1"
    );
  });

  it("does not render Convert to job for LOST lead", () => {
    render(<NextBestActions entityType="lead" status="LOST" leadId="lead-1" />);
    expect(screen.queryByRole("link", { name: /convert to job/i })).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: /create task/i })).toHaveAttribute(
      "href",
      "/app/tasks/new?leadId=lead-1"
    );
  });

  it("does not render Convert to job when lead already converted", () => {
    render(
      <NextBestActions
        entityType="lead"
        status="NEW"
        leadId="lead-1"
        leadConvertedJobId="job-1"
      />
    );
    expect(screen.queryByRole("link", { name: /convert to job/i })).not.toBeInTheDocument();
  });

  it("renders estimate SENT with Set follow-up only when share callbacks omitted", () => {
    render(
      <NextBestActions
        entityType="estimate"
        status="SENT"
        estimateId="est-1"
        jobId="job-1"
        customerId="cust-9"
      />
    );

    expect(screen.queryByRole("link", { name: /share link/i })).not.toBeInTheDocument();
    expect(screen.queryByTestId("nba-estimate-send-email")).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: /set follow-up/i })).toHaveAttribute(
      "href",
      "/app/tasks/new?jobId=job-1&customerId=cust-9"
    );
  });

  it("renders estimate SENT with Send Email, Generate Link, and Set follow-up when share callbacks provided", () => {
    const share = { onSendEmail: jest.fn(), onGenerateLink: jest.fn() };
    render(
      <NextBestActions
        entityType="estimate"
        status="SENT"
        estimateId="est-1"
        jobId="job-1"
        customerId="cust-9"
        estimateShareActions={share}
      />
    );

    expect(screen.getByTestId("nba-estimate-send-email")).toBeInTheDocument();
    expect(screen.getByTestId("nba-estimate-generate-link")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /set follow-up/i })).toHaveAttribute(
      "href",
      "/app/tasks/new?jobId=job-1&customerId=cust-9"
    );
  });

  it("renders estimate SENT Set follow-up with job only when customerId is absent", () => {
    render(
      <NextBestActions entityType="estimate" status="SENT" estimateId="est-1" jobId="job-1" />
    );
    expect(screen.getByRole("link", { name: /set follow-up/i })).toHaveAttribute(
      "href",
      "/app/tasks/new?jobId=job-1"
    );
  });

  it("renders estimate DRAFT with Edit estimate only when share callbacks omitted", () => {
    render(
      <NextBestActions entityType="estimate" status="DRAFT" estimateId="est-1" jobId="job-1" />
    );
    expect(screen.queryByRole("link", { name: /share estimate/i })).not.toBeInTheDocument();
    expect(screen.queryByTestId("nba-estimate-send-email")).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: /edit estimate/i })).toHaveAttribute(
      "href",
      "/app/estimates/est-1/edit"
    );
  });

  it("renders estimate DRAFT with Send Email, Generate Link, and Edit estimate when share callbacks provided", () => {
    const share = { onSendEmail: jest.fn(), onGenerateLink: jest.fn() };
    render(
      <NextBestActions
        entityType="estimate"
        status="DRAFT"
        estimateId="est-1"
        jobId="job-1"
        estimateShareActions={share}
      />
    );
    expect(screen.getByTestId("nba-estimate-send-email")).toBeInTheDocument();
    expect(screen.getByTestId("nba-estimate-generate-link")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /edit estimate/i })).toHaveAttribute(
      "href",
      "/app/estimates/est-1/edit"
    );
  });

  it("renders estimate ACCEPTED action set without redundant View estimate (same page)", () => {
    render(
      <NextBestActions entityType="estimate" status="ACCEPTED" estimateId="est-1" jobId="job-1" />
    );
    expect(screen.queryByRole("link", { name: /^view estimate$/i })).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: /view job/i })).toHaveAttribute("href", "/app/jobs/job-1");
  });

  it("renders estimate REJECTED action set same as ACCEPTED", () => {
    render(
      <NextBestActions entityType="estimate" status="REJECTED" estimateId="est-1" jobId="job-1" />
    );
    expect(screen.queryByRole("link", { name: /^view estimate$/i })).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: /view job/i })).toHaveAttribute("href", "/app/jobs/job-1");
  });

  it("renders job UNSCHEDULED action set with Schedule linking to schedule board with focusJob", () => {
    render(<NextBestActions entityType="job" status="UNSCHEDULED" jobId="job-1" />);

    expect(screen.getByRole("link", { name: /^schedule$/i })).toHaveAttribute(
      "href",
      "/app/schedule?focusJob=job-1"
    );
    expect(screen.getByRole("link", { name: /create estimate/i })).toHaveAttribute(
      "href",
      "/app/jobs/job-1/estimates/new"
    );
    expect(screen.getByRole("link", { name: /create task/i })).toHaveAttribute(
      "href",
      "/app/tasks/new?jobId=job-1"
    );
  });

  it("renders job SCHEDULED action set with Upload photos anchored to attachments", () => {
    render(<NextBestActions entityType="job" status="SCHEDULED" jobId="job-1" />);

    expect(screen.getByRole("link", { name: /upload photos/i })).toHaveAttribute(
      "href",
      "/app/jobs/job-1#attachments"
    );
    expect(screen.getByRole("link", { name: /assign crew/i })).toHaveAttribute(
      "href",
      "/app/jobs/job-1/edit"
    );
  });

  it("renders job IN_PROGRESS action set with View estimates", () => {
    render(<NextBestActions entityType="job" status="IN_PROGRESS" jobId="job-1" />);
    expect(screen.getByRole("link", { name: /view estimates/i })).toHaveAttribute(
      "href",
      "/app/jobs/job-1/estimates"
    );
  });
});
