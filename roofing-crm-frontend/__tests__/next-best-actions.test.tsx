import React from "react";
import { render, screen } from "./test-utils";
import { NextBestActions } from "@/components/NextBestActions";

describe("NextBestActions", () => {
  it("renders lead NEW action set", () => {
    render(<NextBestActions entityType="lead" status="NEW" leadId="lead-1" />);

    expect(screen.getByRole("link", { name: /call customer/i })).toHaveAttribute("href", "/app/leads/lead-1");
    expect(screen.getByRole("link", { name: /schedule inspection/i })).toHaveAttribute(
      "href",
      "/app/tasks/new?leadId=lead-1"
    );
    expect(screen.getByRole("link", { name: /convert to job/i })).toHaveAttribute(
      "href",
      "/app/leads/lead-1/convert"
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

  it("renders estimate SENT action set", () => {
    render(
      <NextBestActions
        entityType="estimate"
        status="SENT"
        estimateId="est-1"
        jobId="job-1"
      />
    );

    expect(screen.getByRole("link", { name: /share link/i })).toHaveAttribute("href", "/app/estimates/est-1");
    expect(screen.getByRole("link", { name: /set follow-up/i })).toHaveAttribute(
      "href",
      "/app/tasks/new?jobId=job-1"
    );
  });

  it("renders job UNSCHEDULED action set", () => {
    render(<NextBestActions entityType="job" status="UNSCHEDULED" jobId="job-1" />);

    expect(screen.getByRole("link", { name: /^schedule$/i })).toHaveAttribute("href", "/app/schedule");
    expect(screen.getByRole("link", { name: /create estimate/i })).toHaveAttribute(
      "href",
      "/app/jobs/job-1/estimates/new"
    );
    expect(screen.getByRole("link", { name: /create task/i })).toHaveAttribute(
      "href",
      "/app/tasks/new?jobId=job-1"
    );
  });
});
