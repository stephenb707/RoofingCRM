import React from "react";
import { render, screen } from "./test-utils";
import userEvent from "@testing-library/user-event";
import { fireEvent } from "@testing-library/react";
import { AttachmentSection } from "@/components/AttachmentSection";

describe("AttachmentSection", () => {
  it("renders attachments with tag badges", () => {
    const attachments = [
      {
        id: "att-1",
        fileName: "damage.jpg",
        contentType: "image/jpeg",
        fileSize: 1024,
        tag: "DAMAGE",
        leadId: "lead-1",
        jobId: null,
      },
      {
        id: "att-2",
        fileName: "invoice.pdf",
        contentType: "application/pdf",
        fileSize: 2048,
        tag: "INVOICE",
        leadId: null,
        jobId: "job-1",
      },
    ];
    const onUpload = jest.fn();
    const onDownload = jest.fn();

    render(
      <AttachmentSection
        title="Attachments"
        attachments={attachments}
        onUpload={onUpload}
        onDownload={onDownload}
      />
    );

    expect(screen.getByText("damage.jpg")).toBeInTheDocument();
    expect(screen.getByText("invoice.pdf")).toBeInTheDocument();
    expect(screen.getAllByText("Damage").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Invoice").length).toBeGreaterThanOrEqual(1);
  });

  it("calls onUpload with file, tag and description when user selects tag and uploads", async () => {
    const onUpload = jest.fn();
    const onDownload = jest.fn();
    const file = new File(["content"], "photo.jpg", { type: "image/jpeg" });

    render(
      <AttachmentSection
        title="Attachments"
        attachments={[]}
        onUpload={onUpload}
        onDownload={onDownload}
      />
    );

    const tagSelect = screen.getByLabelText("Tag");
    await userEvent.selectOptions(tagSelect, "DAMAGE");

    const descInput = screen.getByPlaceholderText("Description (optional)");
    await userEvent.type(descInput, "Roof damage photo");

    const fileInput = screen.getByLabelText("Choose file to upload");
    fireEvent.change(fileInput, { target: { files: [file] } });

    expect(onUpload).toHaveBeenCalledWith(file, {
      tag: "DAMAGE",
      description: "Roof damage photo",
    });
  });
});
